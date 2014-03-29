package nl.ovapi.bison.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.bison.model.VehicleDatabase.VehicleType;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.TooOldException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.gtfsrt.services.GeometryService;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.StopPoint;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.Position.Builder;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiVehiclePosition;

public class JourneyProcessor {


	@Getter
	/**
	 * Last received KV6posinfo for this Journey
	 */
	private KV6posinfo posinfo;

	private Map<Integer, ArrayList<Mutation>> mutations;

	@Getter
	@Setter
	/**
	 * Map with last received posinfo for reinforcementnumber.
	 */
	private Map<Integer, KV6posinfo> reinforcements;

	@Getter private List<DatedPasstime> datedPasstimes;

	private static final Logger _log = LoggerFactory.getLogger(JourneyProcessor.class);

	private Journey _journey;
	public JourneyProcessor(@NonNull Journey journey){
		_journey = journey;
		mutations = Maps.newHashMap();
		reinforcements = Maps.newHashMap();
		datedPasstimes = new ArrayList<DatedPasstime>(journey.getJourneypattern().getPoints().size());
		materializeDatedPasstimes(journey);
	}

	//Speeds used to calculate theoretical fastest speeds
	private static final int DEFAULT_SPEED = (int) (75 / 3.6); // meters per seconds

	private static final int LONGHAUL_SPEED = (int) (90 / 3.6); // meters per seconds
	private final static int LONGHAUL_DISTANCE = 10000; //meters

	private static final int SHORTHAUL_SPEED = (int) (45 / 3.6); // meters per seconds
	private final static int SHORTHAUL_DISTANCE = 1000; //meters

	// Minimum allowed punctuality, filter out very negative punctualities.
	//We don't expect vehicles to drive minutes ahead of schedule.
	private static final int MIN_PUNCTUALITY = -360;

	// Minimum allowed punctuality when departing from timingpoint, in seconds
	private static final int MIN_PROGNOSIS_FROM_TIMINGPOINT = -30; 

	//Punctuality floor, threshold punctuality to regard punctuality as too insignificant to propagate.
	private static final int PUNCTUALITY_FLOOR = 15; // seconds

	// Time it takes to unload a bus at a major stop eg a trainstation.
	private static final int MIN_STOPWAITTIME = 300; //Seconds

	private final static int POSINFO_MAX_AGE = 120;

	/**
	 * @return POSIX time when journey end in seconds since January 1st 1970 00:00:00 UTC
	 */
	public long getEndEpoch(){
		try {
			Calendar c = Calendar.getInstance(TimeZone.getDefault());
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(_journey.getOperatingDay()));
			c.set(Calendar.HOUR, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.SECOND, _journey.getDeparturetime());
			c.add(Calendar.SECOND, _journey.getTimedemandgroup().getPoints().get(_journey.getTimedemandgroup().getPoints().size()-1).getTotaldrivetime());
			if (posinfo != null && posinfo.getPunctuality() != null){
				c.add(Calendar.SECOND, Math.abs(posinfo.getPunctuality()));
			}
			return c.getTimeInMillis()/1000;
		} catch (ParseException e) {
			return -1;
		}
	}

	private TripUpdate.Builder filter(TripUpdate.Builder tripUpdate){
		if (tripUpdate.getStopTimeUpdateCount() == 0)
			return tripUpdate;
		tripUpdate.getStopTimeUpdateOrBuilderList();
		long lastTime = Long.MAX_VALUE;
		for (int i = tripUpdate.getStopTimeUpdateCount()-1; i >= 0; i--){ //Filter negative dwells and stoptimes
			StopTimeUpdate.Builder update = tripUpdate.getStopTimeUpdateBuilder(i);
			if (update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.NO_DATA || 
					update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED ||
					update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate)){
				continue;
			}
			if (i == 0 && (!update.hasDeparture() || !update.hasArrival())){
				//System.out.println(tripUpdate.build());
				//System.out.println(update.build());
				_log.error("Departure or arrival is missing");
			}
			if (update.getDeparture().getTime() > lastTime){
				int offset = (int) (lastTime - update.getDeparture().getTime());
				update.getDepartureBuilder().setTime(update.getDeparture().getTime()+offset);
				update.getDepartureBuilder().setDelay((int)(update.getDepartureBuilder().getTime()-_journey.getDepartureTime(update.getStopSequence())));
			}
			lastTime = update.getDeparture().getTime();
			if (update.getArrival().getTime() > lastTime){
				int offset = (int) (lastTime - update.getArrival().getTime());
				update.getArrivalBuilder().setTime(update.getArrival().getTime()+offset);
				update.getArrivalBuilder().setDelay((int)(update.getArrivalBuilder().getTime()-_journey.getArrivalTime(update.getStopSequence())));
			}
			lastTime = update.getArrival().getTime();
		}
		ArrayList<StopTimeUpdate.Builder> updates = new ArrayList<StopTimeUpdate.Builder>();
		int lastDelay = Integer.MIN_VALUE;
		StopTimeUpdate.ScheduleRelationship lastSchedule = StopTimeUpdate.ScheduleRelationship.SCHEDULED;
		boolean hadStopTimeExtension = false;
		List<StopTimeUpdate.Builder> unfilteredUpdates = tripUpdate.getStopTimeUpdateBuilderList();
		for (int i = 0; i < unfilteredUpdates.size(); i++){
			StopTimeUpdate.Builder update = unfilteredUpdates.get(i);
			if (update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.NO_DATA || 
					update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED ||
					update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate)){
				if (update.hasArrival()){
					update.clearArrival();
				}
				if (update.hasDeparture()){
					update.clearDeparture();
				}
				updates.add(update); //No data
				lastDelay = Integer.MIN_VALUE;
				lastSchedule = update.hasScheduleRelationship() ? StopTimeUpdate.ScheduleRelationship.SCHEDULED :
					update.getScheduleRelationship();
				hadStopTimeExtension = update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
				continue;
			}
			boolean override = lastSchedule != update.getScheduleRelationship() ||
					hadStopTimeExtension != update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
			if (update.hasArrival()){
				if ((update.getArrival().getDelay() == lastDelay && !override ) || 
						(i == 0 && update.getDeparture().getDelay() == update.getArrival().getDelay())){
					update.clearArrival();
				}else{
					lastDelay = update.getArrival().getDelay();
				}
			}
			if (update.hasDeparture()){
				if (update.getDeparture().getDelay() == lastDelay && (i != 0) && !override){
					//update.clearDeparture();
				}else{
					lastDelay = update.getDeparture().getDelay();
				}
			}
			if (update.hasArrival() || (update.hasDeparture() && i == 0)){
				updates.add(update);
			}
			lastSchedule = update.hasScheduleRelationship() ? StopTimeUpdate.ScheduleRelationship.SCHEDULED :
				update.getScheduleRelationship();
			hadStopTimeExtension = update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
		}
		tripUpdate.clearStopTimeUpdate();
		for (StopTimeUpdate.Builder update: updates){
			tripUpdate.addStopTimeUpdate(update);
		}
		return tripUpdate;
	}


	/**
	 * Materialize journey into DatedPasstime's 
	 * @param journey 
	 */
	private void materializeDatedPasstimes(Journey journey){
		long time = System.currentTimeMillis();
		int distanceAtStartTrip = 0;
		for (int i = 0; i < journey.getTimedemandgroup().getPoints().size();i++){
			TimeDemandGroupPoint tpt = journey.getTimedemandgroup().getPoints().get(i);
			JourneyPatternPoint jpt = journey.getJourneypattern().getPoint(tpt.getPointorder());
			DatedPasstime dp = new DatedPasstime();
			if (i == 0){
				distanceAtStartTrip = jpt.getDistancefromstartroute();
				dp.setDistanceDriven(0);
			}else{
				dp.setDistanceDriven(jpt.getDistancefromstartroute()-distanceAtStartTrip);
			}
			String[] privateRef = journey.getPrivateCode().split(":");
			dp.setDataOwnerCode(DataOwnerCode.valueOf(privateRef[0]));
			dp.setLinePlanningNumber(privateRef[1]);
			dp.setJourneyNumber(Integer.parseInt(privateRef[2]));
			if (i == 0){
				dp.setJourneyStopType(JourneyStopType.FIRST);
			}else if (i == journey.getTimedemandgroup().getPoints().size()-1){
				dp.setJourneyStopType(JourneyStopType.LAST);
			}else if (!jpt.isScheduled()){
				dp.setJourneyStopType(JourneyStopType.INFOPOINT);
			}else{
				dp.setJourneyStopType(JourneyStopType.INTERMEDIATE);
			}
			dp.setFortifyOrderNumber(0);
			dp.setTimingPointCode(jpt.getPointref()+"");
			dp.setUserStopCode(jpt.getOperatorpointref());
			dp.setTargetArrivalTime(journey.getDeparturetime()+tpt.getTotaldrivetime());
			dp.setTargetDepartureTime(journey.getDeparturetime()+tpt.getTotaldrivetime()+tpt.getStopwaittime());
			dp.setExpectedArrivalTime(dp.getTargetArrivalTime());
			dp.setExpectedDepartureTime(dp.getTargetDepartureTime());
			dp.setLocalServiceLevelCode((int)journey.getAvailabilityConditionRef().intValue()); //TODO HACK very large overflow risk here
			dp.setDestinationCode(jpt.getDestinationCode());
			dp.setUserStopOrderNumber(tpt.getPointorder());
			if (journey.getWheelchairaccessible() == null){
				dp.setWheelChairAccessible(WheelChairAccessible.UNKNOWN);
			}else if (journey.getWheelchairaccessible()){
				dp.setWheelChairAccessible(WheelChairAccessible.ACCESSIBLE);
			}else{
				dp.setWheelChairAccessible(WheelChairAccessible.NOTACCESSIBLE);
			}
			dp.setOperationDate(journey.getOperatingDay());
			dp.setTimingPointDataOwnerCode(DataOwnerCode.ALGEMEEN);
			dp.setTripStopStatus(TripStopStatus.PLANNED);
			dp.setLineDirection(journey.getJourneypattern().getDirectiontype());
			dp.setTimingStop(jpt.isWaitpoint());
			dp.setJourneyPatternCode(Integer.valueOf(journey.getJourneypattern().getJourneyPatternRef())); //TODO very large overflow risk here
			dp.setSideCode(jpt.getPlatformCode());
			dp.setLastUpdateTimeStamp(time);
			datedPasstimes.add(dp);
		}
	}

	/**
	 * Clear
	 *  KV6 posinfo object.
	 */
	public void clearKV6(){
		posinfo = null;
	}


	/**
	 * Set tripStatus for all datedPasstimes in journey
	 * @param tripStatus
	 */
	private void setTripStatusForJourney(TripStopStatus tripStatus){
		for (DatedPasstime dp : datedPasstimes){
			dp.setTripStopStatus(tripStatus);
		}
	}

	/**
	 * Set reason fields to all datedPasstimes
	 * @param reasonType
	 * @param subReasonType
	 * @param reasonContent
	 */
	private void setReasonForJourney(ReasonType reasonType, SubReasonType subReasonType, String reasonContent){
		for (DatedPasstime dp : datedPasstimes){
			dp.setReasonType(reasonType);
			dp.setSubReasonType(subReasonType);
			dp.setReasonContent(reasonContent);
		}
	}

	/**
	 * Set advice fields to all datedPasstimes
	 * @param adviceType
	 * @param subAdviceType
	 * @param adviceContent
	 */
	private void setAdviceForJourney(AdviceType adviceType, SubAdviceType subAdviceType, String adviceContent){
		for (DatedPasstime dp : datedPasstimes){
			dp.setAdviceType(adviceType);
			dp.setSubAdviceType(subAdviceType);
			dp.setAdviceContent(adviceContent);
		}
	}

	/**
	 * Process KV17MutateJourney mutation
	 * @param UNIX timestamp milliseconds sinds 1970
	 * @param m KV17Mutation message
	 */
	private void parseMutateJourney(Long timestamp, Mutation m) {
		switch (m.getMutationtype()) {
		case CANCEL:
			setTripStatusForJourney(TripStopStatus.CANCEL);
			setReasonForJourney(ReasonType.parse(m.getReasontype()),SubReasonType.parse(m.getSubreasontype()),m.getReasoncontent());
			setAdviceForJourney(AdviceType.parse(m.getAdvicetype()),SubAdviceType.parse(m.getAdvicetype()),m.getAdvicetype());
			_journey.setCanceled(true);
			break;
		case RECOVER:
			clearKV17mutations();
			//Set UNKNOWN if past departure time
			//Set PLANNED if before
			setTripStatusForJourney(System.currentTimeMillis() > _journey.getDepartureEpoch() ? 
					TripStopStatus.UNKNOWN : TripStopStatus.PLANNED);
			_journey.setCanceled(false);
			break;
		default:
			break;
		}
	}

	/**
	 * Remove all modifications made by KV17.
	 */
	private void clearKV17mutations(){
		for (DatedPasstime dp : datedPasstimes){
			if (dp.getTripStopStatus() == TripStopStatus.CANCEL){
				dp.setTripStopStatus(TripStopStatus.PLANNED);
			}
			dp.setAdviceContent(null);
			dp.setAdviceType(null);
			dp.setSubAdviceType(null);
			dp.setReasonContent(null);
			dp.setReasonType(null);
			dp.setSubReasonType(null);
			dp.setMessageContent(null);
			dp.setMessageType(null);
			dp.setLag(0);
		}
	}

	private void parseMutateJourneyStop(Long timestamp, Mutation m)
			throws StopNotFoundException {

		int passageSequence = 0; //Counter for how many times we came across the userstopcode in posinfo
		for (DatedPasstime dp : datedPasstimes){
			boolean userStopMatches = dp.getUserStopCode().equals(m.getUserstopcode());
			if (userStopMatches && passageSequence == m.getPassagesequencenumber()){ 
				switch (m.getMutationtype()) {
				case MUTATIONMESSAGE:
					dp.setAdviceType(AdviceType.parse(m.getAdvicetype()));
					dp.setSubAdviceType(SubAdviceType.parse(m.getSubadvicetype()));
					dp.setAdviceContent(m.getAdvicecontent());

					dp.setReasonType(ReasonType.parse(m.getReasontype()));
					dp.setSubReasonType(SubReasonType.parse(m.getSubreasontype()));
					dp.setReasonContent(m.getReasoncontent());
					break;
				case CHANGEDESTINATION://Not supported by Koppelvlak78
					break;
				case CHANGEPASSTIMES:
					//TODO
					break;
				case LAG:
					dp.setLag(m.getLagtime());
					break;
				case RECOVER:
					m.setLagtime(0);
					setTripStatusForJourney(System.currentTimeMillis() > _journey.getDepartureEpoch() ? 
							TripStopStatus.UNKNOWN : TripStopStatus.PLANNED);
					break;
				case CANCEL:
				case SHORTEN:
					dp.setTripStopStatus(TripStopStatus.CANCEL);
					break;
				default:
					_log.info("Unknown mutationtype {}",m);
					break;
				}
			}else if (userStopMatches){
				passageSequence++;
			}
		}
	}

	public FeedEntity vehiclePosition(String id,JourneyProcessor journey,KV6posinfo posinfo,RIDservice ridService,GeometryService geomService){
		switch(posinfo.getMessagetype()){
		case DELAY:
		case END://These messagetype do not contain vehicle-position information
			return null;
		default:
			break;
		}
		FeedEntity.Builder feedEntity = FeedEntity.newBuilder();
		feedEntity.setId(id);
		VehiclePosition.Builder vehiclePosition = VehiclePosition.newBuilder();
		int delay = posinfo.getPunctuality() == null ? 0 : posinfo.getPunctuality();

		int passageSequence = 0; //Counter for how many times we came across the userstopcode in posinfo

		for (int i = 0; i < datedPasstimes.size();i++){
			DatedPasstime dp = datedPasstimes.get(i);
			boolean userStopMatches = dp.getUserStopCode().equals(posinfo.getUserstopcode());
			if (userStopMatches && passageSequence == posinfo.getPassagesequencenumber()){
				//Find datedpasstime of next scheduled stoppoint
				DatedPasstime dpNext = null;
				SCAN_NEXT : for (int j = i+1; j < datedPasstimes.size();j++){
					if (datedPasstimes.get(j).getJourneyStopType() != JourneyStopType.INFOPOINT){
						dpNext = datedPasstimes.get(i); // First non Dummy stop
						break SCAN_NEXT;
					}
				}
				switch (posinfo.getMessagetype()){
				case ARRIVAL:
				case ONSTOP:
				case INIT:
					vehiclePosition.setCurrentStatus(VehicleStopStatus.STOPPED_AT);
					vehiclePosition.setCurrentStopSequence(dp.getUserStopOrderNumber());
					StopPoint sp = ridService.getStopPoint(Long.valueOf(dp.getTimingPointCode()));
					if (sp != null){
						Builder position = Position.newBuilder();
						position.setLatitude(sp.getLatitude());
						position.setLongitude(sp.getLongitude());
						vehiclePosition.setPosition(position);
					}
					break;
				case DEPARTURE: //Set location of stop
					sp = ridService.getStopPoint(Long.valueOf(dp.getTimingPointCode()));
					if (sp != null){
						Builder position = Position.newBuilder();
						position.setLatitude(sp.getLatitude());
						position.setLongitude(sp.getLongitude());
						vehiclePosition.setPosition(position);
					}
				case OFFROUTE:
				case ONROUTE:
					if (dpNext == null){
						return null;
					}
					vehiclePosition.setCurrentStatus(VehicleStopStatus.IN_TRANSIT_TO);
					vehiclePosition.setStopId(dpNext.getTimingPointCode());
					vehiclePosition.setCurrentStopSequence(dpNext.getUserStopOrderNumber());
					break;
				default:
					return null;
				}
				if (posinfo.getRd_x() != null){
					Position position = geomService.toWGS84(posinfo.getRd_x(), posinfo.getRd_y());
					if (position != null)
						vehiclePosition.setPosition(position);
				}
				TripDescriptor.Builder tripDescription = _journey.tripDescriptor();
				if (posinfo.getReinforcementnumber() > 0){
					tripDescription.setScheduleRelationship(ScheduleRelationship.ADDED);
				}
				//Set punctuality in OVapi extension
				if (posinfo.getPunctuality() != null){
					OVapiVehiclePosition.Builder ovapiVehiclePosition = OVapiVehiclePosition.newBuilder();
					if (vehiclePosition.hasCurrentStopSequence() && vehiclePosition.getCurrentStopSequence() <= 1 && delay < 0){
						delay = 0;
					}
					ovapiVehiclePosition.setDelay(delay);
					vehiclePosition.setExtension(GtfsRealtimeOVapi.ovapiVehiclePosition, ovapiVehiclePosition.build());
				}
				feedEntity.setVehicle(vehiclePosition);
				return feedEntity.build();
			}else if (userStopMatches){
				passageSequence++;
			}
		}
		return null;
	}	

	public TripUpdate.Builder update(ArrayList<KV17cvlinfo> cvlinfos) throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException {
		long timestamp = 0;
		if (cvlinfos.size() == 0){
			return null;
		}
		mutations.clear();
		_journey.setCanceled(false);

		//KV17 mutations are not differential, remove possible previous modifications.
		clearKV17mutations();

		for (KV17cvlinfo cvlinfo : cvlinfos) {
			SCAN_MUTATIONS: for (Mutation mut : cvlinfo.getMutations()) {
				try {
					timestamp = Math.max(timestamp, cvlinfo.getTimestamp());
					switch (mut.getMessagetype()) {
					case KV17MUTATEJOURNEY:
						parseMutateJourney(cvlinfo.getTimestamp(), mut);
						continue SCAN_MUTATIONS;
					case KV17MUTATEJOURNEYSTOP:
						parseMutateJourneyStop(cvlinfo.getTimestamp(), mut);
						continue SCAN_MUTATIONS;
					}
				} catch (Exception e) {
					_log.error("Error applying KV17",e);
				}
			}
		}
		int posinfoAge = (posinfo == null) ? Integer.MAX_VALUE : 
			(int)(Utils.currentTimeSecs()-posinfo.getTimestamp());
		if (timestamp == 0)
			timestamp = Utils.currentTimeSecs();
		if (posinfo != null && posinfoAge < POSINFO_MAX_AGE){
			setPunctuality(posinfo);
			return update(posinfo);
		}else{
			KV6posinfo posinfo = new KV6posinfo();
			posinfo.setMessagetype(Type.DELAY); //Fake KV6posinfo to get things moving
			posinfo.setPunctuality(0);
			posinfo.setTimestamp(Utils.currentTimeSecs());
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
			posinfo.setOperatingday(format.format(new Date()));
			setPunctuality(posinfo);
			return update(posinfo);
		}
	}

	/**
	 * Set recorded arrival/departure time using timestamp of departure /arrival trigger
	 *
	 * Reset all recorded times to null when (re)INIT message is received. 
	 * Also set meta-data in a INIT KV6 message to the passtimes
	 * 
	 * @param posinfo KV6posinfo object
	 */
	private void setRecordedTimes(KV6posinfo posinfo){
		switch (posinfo.getMessagetype()){
		case ARRIVAL:
		case DEPARTURE:
			break;
		case INIT: //Clear all recorded times
		default:
			return;
		}
		try{
			for (DatedPasstime dp : datedPasstimes){
				if (dp.getUserStopCode().equals(posinfo.getUserstopcode())){
					SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
					Date operatingDate = format.parse(dp.getOperationDate());
					int recordedTime = (int) ((posinfo.getTimestamp()-operatingDate.getTime()/1000)); //Seconds since Midnight
					if (posinfo.getMessagetype() == Type.ARRIVAL){
						dp.setRecordedArrivalTime(recordedTime);
					}else if (posinfo.getMessagetype() == Type.DEPARTURE){
						/*if the current stop is a timingpoint, filter out significant negative punctualities
						  This is done to filter false departure signals, where a vehicle falsely claims to have departed.
						 */
						int delay = dp.getTargetDepartureTime()-recordedTime;
						if (dp.isTimingStop() || dp.getJourneyStopType() == JourneyStopType.FIRST){
							if (delay < MIN_PROGNOSIS_FROM_TIMINGPOINT){
								break;//Ignore trigger
							}
						}else if (delay < MIN_PUNCTUALITY){
							break;//Ignore trigger
						}else{
							dp.setRecordedDepartureTime(recordedTime);
						}
					}
					break;
				}
			}
		}catch (Exception e){ //Exceptions are relatively impossible
			e.printStackTrace();
		}
	}

	/**
	 * Initialize trip with values from KV6posinfo INIT message
	 * Set wheelchairaccessibility,NumberOfCoaches from INIT message  
	 * @param posinfo
	 */
	private void initTrip(KV6posinfo posinfo){
		if (posinfo.getMessagetype() != Type.INIT){
			return;
		}
		VehicleType type = VehicleDatabase.vehicleType(posinfo);
		WheelChairAccessible accessible = null;
		if (type != null){
			if (type.isWheelchairAccessible()){
				accessible = WheelChairAccessible.ACCESSIBLE;
			}else{
				accessible = WheelChairAccessible.NOTACCESSIBLE;
			}
		}
		for (DatedPasstime pt : datedPasstimes){
			if (pt.getRecordedArrivalTime() != null || pt.getRecordedDepartureTime() != null){
				pt.setRecordedArrivalTime(null);
				pt.setRecordedDepartureTime(null);
				pt.setLastUpdateTimeStamp(posinfo.getTimestamp());
				if (accessible != null)
					pt.setWheelChairAccessible(accessible);
				pt.setNumberOfCoaches(posinfo.getNumberofcoaches());
			}
		}
	}

	/**
	 * Set TripStopStatus for each DatedPasstime.
	 * @param posinfo KV6posinfo object
	 */
	private void setTripStatus(KV6posinfo posinfo){
		//beforeCurrent: we're scanning prior to the current stop in Posinfo
		//Delay messages are always "before the current stop"
		boolean beforeCurrent = posinfo.getMessagetype() != Type.DELAY; 

		int passageSequence = 0; //Counter for how many times we came across the userstopcode in posinfo

		for (DatedPasstime dp : datedPasstimes){
			boolean userStopMatches = dp.getUserStopCode().equals(posinfo.getUserstopcode());
			if (userStopMatches && passageSequence == posinfo.getPassagesequencenumber()){ 
				switch (posinfo.getMessagetype()){
				case DEPARTURE: //Vehicle departed from this stop
				case ONROUTE: //Vehicle is driving away from this stop
				case OFFROUTE: //Vehicle is driving away from this stop, but off planned route
					if (dp.getTripStopStatus() != TripStopStatus.CANCEL)
						dp.setTripStopStatus(TripStopStatus.PASSED);
					break;

				case ARRIVAL: //Vehicle arrived at this stop
				case ONSTOP: //Vehicle is halted at this stop
					if (dp.getTripStopStatus() != TripStopStatus.CANCEL)
						dp.setTripStopStatus(TripStopStatus.ARRIVED);
					break;
				default: //Shouldn't happen as all other messageType's do not contain userstopcode
					break;
				}
				beforeCurrent = false;
			}else if (beforeCurrent){
				if (userStopMatches){
					passageSequence++; //We're on a stop with the same userstopcode, but the KV6posinfo is referring to the same stop on a later passage 
				}
				if (dp.getTripStopStatus() != TripStopStatus.CANCEL)
					dp.setTripStopStatus(TripStopStatus.PASSED);
			}else{//Now we're scanning stops not yet visited
				if (dp.getTripStopStatus() != TripStopStatus.CANCEL)
					dp.setTripStopStatus(TripStopStatus.DRIVING);
			}
		}
	}

	/**
	 * Set estimated times using the punctuality in posinfo for stop and make prognoses for subsequent stops
	 * @param posinfo KV6posinfo object
	 */

	public void setPunctuality(KV6posinfo posinfo){
		switch (posinfo.getMessagetype()){
		case INIT: //No punctuality information
		case END://No punctuality information
		case OFFROUTE: //No punctuality information
		case ARRIVAL://No punctuality information
		default:
			return;
		case DELAY:
		case DEPARTURE:
		case ONROUTE:
		case ONSTOP:
			break;
		}
		// We first need to scan all the stops in the journey, to set prognoses
		// Unless it's a KV6 delay message
		Integer prognosis = posinfo.getMessagetype() == Type.DELAY ? posinfo.getPunctuality() : null;

		//Time since start journey, at the pointsince start journey, at which the punctuality starts for with the punctuality is estimated in KV6
		//Used to decay KV6 punctuality using simple time decay.
		int timeAtCurrentKV6Stop = 0;

		int passageSequence = 0; //Counter for how many times we came across the userstopcode in posinfo
		for (int i = 0; i < datedPasstimes.size();i++){
			DatedPasstime dp = datedPasstimes.get(i);
			//next datedPasstime, null on last stop.
			DatedPasstime dpNext = (i != datedPasstimes.size()-1) ? datedPasstimes.get(i+1) : null;

			boolean userStopMatches = dp.getUserStopCode().equals(posinfo.getUserstopcode());
			if (userStopMatches && passageSequence == posinfo.getPassagesequencenumber()){ 
				prognosis = posinfo.getPunctuality(); //Set initial prognosis for following stops

				//set time to possibly use in simple timedecay 
				timeAtCurrentKV6Stop = dp.getTargetArrivalTime();

				/*if the current stop is a timingpoint, filter out significant negative punctualities
				  This is done to filter false departure signals, where a vehicle falsely claims to have departed.
				 */
				if (dp.isTimingStop() || dp.getJourneyStopType() == JourneyStopType.FIRST){
					if (prognosis < MIN_PROGNOSIS_FROM_TIMINGPOINT){
						prognosis = 0;
					}
				}else if (prognosis < MIN_PUNCTUALITY){
					//Set prognosis to 0 if vehicle has a too large punctuality.
					prognosis = 0;
				}
			}else if (prognosis == null && userStopMatches){
				passageSequence++; //We're on a stop with the same userstopcode, but the KV6posinfo is referring to the same stop on a later passage 
			}else if (prognosis != null){//Now we're scanning stops not yet visited and have a prognosis
				if (Math.abs(prognosis) < PUNCTUALITY_FLOOR){
					prognosis = 0; //Punctuality is thus low, it's no longer significant enough to propagate  
				}
				dp.setExpectedArrivalTime(dp.getTargetArrivalTime()+prognosis);
				int stopWaitTime = dp.getTargetDepartureTime()-dp.getTargetArrivalTime();
				if (dp.isTimingStop() && prognosis < 0){
					prognosis = 0; //This is a timingstop, vehicles are not expected to depart early.
				}else if (stopWaitTime > MIN_STOPWAITTIME){
					//Dwell-time cq stopwaittime is larger than the minimum set, use comfort zone to reduce delay
					int dwellComfort = stopWaitTime-MIN_STOPWAITTIME;
					prognosis -= dwellComfort;		
				}

				if (dp.getLag() != null && dp.getLag() >= 0){
					//Lag mutation via KV17, delay is at minimum the lag time
					prognosis = Math.min(prognosis, dp.getLag());
				}

				dp.setExpectedDepartureTime(dp.getTargetDepartureTime()+prognosis);
				if (Math.abs(prognosis) > PUNCTUALITY_FLOOR && dpNext != null){
					if (prognosis < 0){ 
						//Negative punctuality
						int driveTime = dpNext.getTargetArrivalTime()-dp.getTargetDepartureTime();
						int theoreticalMinDriveTime = theoreticalMinDriveTime(dpNext.getDistanceDriven()-dp.getDistanceDriven());
						if (driveTime > theoreticalMinDriveTime){
							// Use too fast legs to reduce negative punctuality.
							prognosis = decayByDistance(prognosis,
									dpNext.getTargetArrivalTime()-dp.getTargetDepartureTime(),
									dpNext.getDistanceDriven()-dp.getDistanceDriven());
						}else{
							// Use simple time decaying to decay the delay
							prognosis = decayByTime(prognosis,dpNext.getTargetArrivalTime()-timeAtCurrentKV6Stop);
						}
					}else if (prognosis > 0){
						//Positive punctuality, decay using delta between theoretical and planned drivetime
						int distance = dpNext.getDistanceDriven()-dp.getDistanceDriven();
						prognosis = decayByDistance(prognosis,
								dpNext.getTargetArrivalTime()-dp.getTargetDepartureTime(),distance);
					}
				}
			}
		}
	}

	/**
	 * @param driveDistance distance in meters between current_stop and next stop
	 * @return theoretical min drivetime in seconds
	 */

	private int theoreticalMinDriveTime(int driveDistance){
		int speed = DEFAULT_SPEED;
		if (driveDistance < SHORTHAUL_DISTANCE){
			speed = SHORTHAUL_SPEED;
		}else if (driveDistance > LONGHAUL_DISTANCE){
			speed = LONGHAUL_SPEED;
		}
		return driveDistance / speed;
	}

	/**
	 * Decay delays using Ttheoretical_fastest - Tplanned difference(comfortzone)
	 * @param delay deviation in seconds from targettime
	 * @param plannedDriveTime drivetime in seconds between current_stop and next stop
	 * @param driveDistance distance in meters between current_stop and next stop
	 * @return
	 */
	private int decayByDistance(int delay,int plannedDriveTime,int driveDistance){
		if (driveDistance == 0){
			_log.trace("Drive distance 0");
			return delay;
		}
		int theoreticalMinDriveTime = theoreticalMinDriveTime(driveDistance);
		int newDelay = delay-(plannedDriveTime-theoreticalMinDriveTime);

		//If the delay is smaller than the comfortzone scheduled, return 0
		if (delay >= 0 && newDelay < 0){
			return 0;
		}else if (delay <= 0 && newDelay > 0){
			return 0;
		}
		return newDelay;
	}

	/**
	 * Decay delay using simple timedecay 
	 * @param delay deviation in seconds from targettime
	 * @param elapsedTime time between current stop and next stop
	 * @return decayed delay at next stop
	 */
	private int decayByTime(int delay,int elapsedTime){
		if (delay == 0)
			return 0;
		double vlamba = 1.0 / 500.0;
		double decay = Math.exp(-vlamba * elapsedTime);
		int decayeddelay = (int) (decay * delay);
		return decayeddelay;
	}

	/**
	 * @return
	 * @throws ParseException
	 */
	public TripUpdate.Builder tripUpdateFromKV8(){
		TripUpdate.Builder trip = TripUpdate.newBuilder();
		trip.setTrip(_journey.tripDescriptor());
		for (DatedPasstime dp : datedPasstimes){
			if (dp.getJourneyStopType() == JourneyStopType.INFOPOINT){
				continue;
			}
			StopTimeUpdate.Builder stop = StopTimeUpdate.newBuilder();
			stop.setStopSequence(dp.getUserStopOrderNumber());
			stop.setStopId(dp.getTimingPointCode());
			switch (dp.getTripStopStatus()){
			case CANCEL:
				stop.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
				break;
			case DRIVING:
			case ARRIVED:
				stop.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
				break;
			case OFFROUTE:
			case UNKNOWN:
				stop.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
				break;
			case PASSED:
			case PLANNED:
				break;
			}
			StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
			if (dp.getRecordedArrivalTime() != null){
				arrival.setTime(secondsSince1970(dp.getOperationDate(),dp.getRecordedArrivalTime()));
				arrival.setDelay((dp.getRecordedArrivalTime()-dp.getTargetArrivalTime()));
			}else{
				arrival.setTime(secondsSince1970(dp.getOperationDate(),dp.getExpectedArrivalTime()));
				arrival.setDelay((dp.getExpectedArrivalTime()-dp.getTargetArrivalTime()));
			}
			stop.setArrival(arrival);
			StopTimeEvent.Builder departure = StopTimeEvent.newBuilder();
			if (dp.getRecordedDepartureTime() != null){
				departure.setDelay((dp.getRecordedDepartureTime()-dp.getTargetDepartureTime()));
				departure.setTime(secondsSince1970(dp.getOperationDate(),dp.getRecordedDepartureTime()));
			}else{
				departure.setDelay((dp.getExpectedDepartureTime()-dp.getTargetDepartureTime()));
				departure.setTime(secondsSince1970(dp.getOperationDate(),dp.getExpectedDepartureTime()));
			}
			stop.setDeparture(departure);
			trip.addStopTimeUpdate(stop);
		}
		return trip;
	}

	public int secondsSince1970(String operatingDate, int secondsSinceMidnight){
		Calendar c = Calendar.getInstance(TimeZone.getDefault());
		try{
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(_journey.getOperatingDay()));
		}catch (Exception e){
			_log.error("Parse operatingday fail {}",_journey.getOperatingDay());
		}
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		return (int) (c.getTimeInMillis()/1000)+secondsSinceMidnight;
	}

	public TripUpdate.Builder update(KV6posinfo posinfo) throws StopNotFoundException,UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException {
		return update(posinfo,false);
	}

	public TripUpdate.Builder update(KV6posinfo posinfo,boolean printKV8) throws StopNotFoundException,UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException {
		long currentTime = Utils.currentTimeSecs();
		if (posinfo.getTimestamp()<currentTime-POSINFO_MAX_AGE){
			throw new TooOldException(posinfo.toString());
		}
		long departureTime = _journey.getDepartureEpoch();
		if (!posinfo.getOperatingday().equals(datedPasstimes.get(0).getOperationDate())){
			throw new IllegalArgumentException("Wrong date");
		}
		if (currentTime < departureTime){
			int timeDeltaSeconds = (int)(departureTime-Utils.currentTimeSecs());
			if (timeDeltaSeconds>=3600 && posinfo.getMessagetype() != Type.INIT){
				throw new TooEarlyException(posinfo.toString());
			}
		}
		if (posinfo.getUserstopcode() != null
				&& !_journey.getJourneypattern().contains(posinfo.getUserstopcode())) {
			throw new StopNotFoundException(posinfo.toString());
		}
		setRecordedTimes(posinfo);
		if (this.posinfo == null || posinfo.getTimestamp() >= this.posinfo.getTimestamp()){  //This condition makes sure we're not overriding good information with out-of-sequence/old position info's
			if (posinfo.getMessagetype() == Type.INIT)
				initTrip(posinfo);
			setTripStatus(posinfo);
			setPunctuality(posinfo);
			this.posinfo = posinfo;
			/*StringBuilder sb = new StringBuilder();
			for (DatedPasstime dp : datedPasstimes){
				sb.append(dp.toCtxLine());
				sb.append("\n");
			}
			System.out.println(sb.toString());*/
		}
		return filter(tripUpdateFromKV8());
	}		
}
