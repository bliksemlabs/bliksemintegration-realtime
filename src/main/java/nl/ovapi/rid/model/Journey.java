package nl.ovapi.rid.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.ovapi.bison.model.KV17cvlinfo;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation.MutationType;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.TooOldException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiStopTimeUpdate;
@ToString()
public class Journey {
	@Getter
	@Setter
	private Long id;
	@Getter
	@Setter
	private JourneyPattern journeypattern;
	@Getter
	@Setter
	private TimeDemandGroup timedemandgroup;
	@Getter
	@Setter
	private Map<Integer, Long> realizedArrivals;
	@Getter
	@Setter
	private Map<Integer, Long> realizedDepartures;

	@Getter
	@Setter
	private Integer departuretime;
	@Getter
	@Setter
	private Boolean wheelchairaccessible;
	@Getter
	@Setter
	private String agencyId;
	@Getter
	@Setter
	private String operatingDay;

	@Getter
	private KV6posinfo posinfo;

	@Getter
	@Setter
	private boolean isCanceled;

	private Map<Integer, ArrayList<Mutation>> mutations;

	@Getter
	@Setter
	private Map<Integer, KV6posinfo> reinforcements;

	public Journey() {
		mutations = Maps.newHashMap();
		reinforcements = Maps.newHashMap();
		realizedArrivals = Maps.newHashMap();
		realizedDepartures = Maps.newHashMap();
	}

	public void clearKV6(){
		posinfo = null;
	}

	private static final boolean RECORD_TIMES = true;

	private static final int PUNCTUALITY_FLOOR = 15; // seconds
	private static final int DEFAULT_SPEED = (int) (75 / 3.6); // meters per
	// seconds
	private static final int LONGHAUL_SPEED = (int) (95 / 3.6); // meters per
	// seconds
	private static final int SHORTHAUL_SPEED = (int) (45 / 3.6); // meters per
	// seconds

	private static final int MIN_PUNCTUALITY = -300; // Minimum allowed
	// punctuality.

	private static final Logger _log = LoggerFactory.getLogger(Journey.class);

	public TripDescriptor.Builder tripDescriptor(){
		TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();
		tripDescriptor.setStartDate(operatingDay.replace("-", ""));
		tripDescriptor.setTripId(id.toString());
		tripDescriptor.setScheduleRelationship(isCanceled ? ScheduleRelationship.CANCELED : ScheduleRelationship.SCHEDULED);
		return tripDescriptor;
	}

	public StopTimeEvent.Builder stopTimeEventArrival(TimeDemandGroupPoint tpt,JourneyPatternPoint pt, int punctuality){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		stopTimeEvent.setDelay(punctuality);
		stopTimeEvent.setTime(getDepartureEpoch()+tpt.getTotaldrivetime()+punctuality);
		return stopTimeEvent;
	}

	public StopTimeEvent.Builder stopTimeEventArrivalRecorded(TimeDemandGroupPoint tpt, long time){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		stopTimeEvent.setTime(time);
		long targettime = getDepartureEpoch()+tpt.getTotaldrivetime();
		stopTimeEvent.setDelay((int)(time-targettime));
		return stopTimeEvent;
	}

	public boolean hasMutations(){
		return mutations.size() > 0 || isCanceled;
	}


	public StopTimeEvent.Builder stopTimeEventDeparture(TimeDemandGroupPoint tpt,JourneyPatternPoint pt, int punctuality){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		if (mutations.containsKey(pt.getPointorder())){
			for (Mutation m : mutations.get(pt.getPointorder())){
				if (m.getMutationtype() == MutationType.LAG){
					punctuality = Math.max(punctuality, m.getLagtime());
				}
			}
		}
		stopTimeEvent.setDelay(punctuality);
		stopTimeEvent.setTime(getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime()+punctuality);
		return stopTimeEvent;
	}

	public StopTimeEvent.Builder stopTimeEventDepartureRecorded(TimeDemandGroupPoint tpt, long time){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		stopTimeEvent.setTime(time);
		long targettime = getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
		stopTimeEvent.setDelay((int)(time-targettime));
		return stopTimeEvent;
	}

	/**
	 * @return POSIX time when journey end in seconds since January 1st 1970 00:00:00 UTC
	 */

	public long getEndEpoch(){
		try {
			Calendar c = Calendar.getInstance(TimeZone.getDefault());
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(getOperatingDay()));
			c.set(Calendar.HOUR, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.SECOND, getDeparturetime());
			c.add(Calendar.SECOND, timedemandgroup.getPoints().get(timedemandgroup.getPoints().size()-1).getTotaldrivetime());
			if (posinfo != null && posinfo.getPunctuality() != null){
				c.add(Calendar.SECOND, Math.abs(posinfo.getPunctuality()));
			}
			return c.getTimeInMillis()/1000;
		} catch (ParseException e) {
			return -1;
		}
	}

	/**
	 * @return POSIX time when journey is scheduled to start in seconds since January 1st 1970 00:00:00 UTC
	 */

	public long getDepartureEpoch(){
		try {
			Calendar c = Calendar.getInstance(TimeZone.getDefault());
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(getOperatingDay()));
			c.set(Calendar.HOUR, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.SECOND, getDeparturetime());
			return c.getTimeInMillis()/1000;
		} catch (ParseException e) {
			return -1;
		}
	}

	private StopTimeUpdate.Builder recordedTimes(TimeDemandGroupPoint tpt, JourneyPatternPoint pt,int lastDelay){
		if (!RECORD_TIMES)
			return null;
		StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
		boolean stopcanceled = isCanceled;
		boolean destChanged = false;
		if (mutations.containsKey(pt.getPointorder())){ // Check if mutation exists with cancel
			for (Mutation m : mutations.get(pt.getPointorder())){
				if (m.getMutationtype() == MutationType.SHORTEN){
					stopcanceled = true;
				}
				if (m.getMutationtype() == MutationType.CHANGEDESTINATION && !destChanged){
					destChanged = true;
					String destination = m.getDestinationname50();
					if (destination == null){
						destination = m.getDestinationname16();
					}
					if (destination == null){
						destination = m.getDestinationdisplay16();
					}
					if (destination != null){
						OVapiStopTimeUpdate.Builder ovapiStopTimeUpdate = OVapiStopTimeUpdate.newBuilder();
						ovapiStopTimeUpdate.setStopHeadsign(destination);
						stopTimeUpdate.setExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate, ovapiStopTimeUpdate.build());
					}
				}
			}
		}
		stopTimeUpdate.setStopSequence(pt.getPointorder());
		stopTimeUpdate.setStopId(pt.getPointref().toString());
		if (!pt.isScheduled())
			return null; // Dummy point
		if (stopcanceled){
			stopTimeUpdate.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
			return stopTimeUpdate;
		}
		if (realizedArrivals.containsKey(pt.getPointorder()))
			stopTimeUpdate.setArrival(stopTimeEventArrivalRecorded(tpt,realizedArrivals.get(pt.getPointorder())));
		if (realizedDepartures.containsKey(pt.getPointorder()))
			stopTimeUpdate.setDeparture(stopTimeEventDepartureRecorded(tpt,realizedDepartures.get(pt.getPointorder())));
		else if (Integer.MIN_VALUE != lastDelay){
			stopTimeUpdate.setDeparture(stopTimeEventDepartureRecorded(tpt,getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime()+lastDelay));
		}else{
			StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
			long targettime = getDepartureEpoch()+tpt.getTotaldrivetime();
			stopTimeEvent.setTime(targettime);
			stopTimeEvent.setDelay(0);
			stopTimeUpdate.setArrival(stopTimeEvent);
			targettime += tpt.getStopwaittime();
			stopTimeEvent.setTime(targettime);
			stopTimeEvent.setDelay(0);
			stopTimeUpdate.setDeparture(stopTimeEvent);
		}
		if (stopTimeUpdate.hasDeparture() && !stopTimeUpdate.hasArrival()){
			StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
			long time = stopTimeUpdate.getDeparture().getTime();
			int delay = stopTimeUpdate.getDeparture().getDelay();
			stopTimeEvent.setTime(time);
			stopTimeEvent.setDelay(delay);
			stopTimeUpdate.setArrival(stopTimeEvent);
		}
		return stopTimeUpdate;
	}

	public TripUpdate.Builder filter(TripUpdate.Builder tripUpdate){
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
			if (!update.hasDeparture() || !update.hasArrival()){
				_log.error("Departure or arrival is missing");
			}
			if (update.getDeparture().getTime() > lastTime){
				int delay = (int) (lastTime - update.getDeparture().getTime());
				if (delay < 0){
					delay -= 1;
				}
				update.getDepartureBuilder().setDelay(delay);
				update.getDepartureBuilder().setTime(update.getDeparture().getTime()+delay);
				lastTime = update.getDeparture().getTime();
			}
			lastTime = update.getDeparture().getTime();
			if (update.getArrival().getTime() > lastTime){
				int delay = (int) (lastTime - update.getArrival().getTime());
				if (delay < 0){
					delay -= 1;
				}
				update.getArrivalBuilder().setDelay(delay);
				update.getArrivalBuilder().setTime(update.getArrival().getTime()+delay);
			}
			lastTime = update.getArrival().getTime();
		}
		ArrayList<StopTimeUpdate.Builder> updates = new ArrayList<StopTimeUpdate.Builder>();
		int lastDelay = Integer.MIN_VALUE;
		for (StopTimeUpdate.Builder update : tripUpdate.getStopTimeUpdateBuilderList()){
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
				continue;
			}
			if (update.hasArrival()){
				if (update.getArrival().getDelay() == lastDelay){
					update.clearArrival();
				}else{
					lastDelay = update.getArrival().getDelay();
				}
			}
			if (update.hasDeparture()){
				if (update.getDeparture().getDelay() == lastDelay){
					update.clearDeparture();
				}else{
					lastDelay = update.getDeparture().getDelay();
				}
			}
			if (update.hasArrival() || update.hasDeparture()){
				updates.add(update);
			}
		}
		tripUpdate.clearStopTimeUpdate();
		for (StopTimeUpdate.Builder update: updates){
			tripUpdate.addStopTimeUpdate(update);
		}
		return tripUpdate;
	}

	private int lastDelay(StopTimeUpdate.Builder stoptimeUpdate,int lastDelay){
		if (stoptimeUpdate.hasDeparture()){
			return stoptimeUpdate.getDeparture().getDelay();
		}
		if (stoptimeUpdate.hasArrival()){
			return stoptimeUpdate.getArrival().getDelay();
		}
		return lastDelay;
	}

	public TripUpdate.Builder updateTimes(KV6posinfo posinfo) {
		boolean passed = posinfo.getMessagetype() != KV6posinfo.Type.DELAY;
		int punctuality = Math
				.max(MIN_PUNCTUALITY, posinfo.getPunctuality() == null ? 0
						: posinfo.getPunctuality());
		TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();
		tripUpdate.setTrip(tripDescriptor());
		tripUpdate.setTimestamp(posinfo.getTimestamp());
		if (isCanceled){
			return tripUpdate;
		}
		VehicleDescriptor.Builder vehicleDesc = posinfo.getVehicleDescription();
		if (vehicleDesc != null)
			tripUpdate.setVehicle(vehicleDesc);
		switch (posinfo.getMessagetype()) {// These types do not contain // information regarding punctuality
		case INIT:
			realizedArrivals = Maps.newHashMap();
			realizedDepartures = Maps.newHashMap();
		case END:
		case OFFROUTE:
			if (getPosinfo() != null && !hasMutations() && getPosinfo().getMessagetype() != Type.OFFROUTE)
				return null; //We've already sent out NO_DATE
			int lastDelay = Integer.MIN_VALUE;
			for (int i = 0; i < timedemandgroup.getPoints().size(); i++) {
				TimeDemandGroupPoint tpt = timedemandgroup.getPoints().get(i);
				JourneyPatternPoint pt = journeypattern.getPoint(tpt.pointorder);
				if (!pt.isScheduled())
					continue;
				StopTimeUpdate.Builder recordedTimes = recordedTimes(tpt,pt,lastDelay);
				if (recordedTimes != null){
					lastDelay = lastDelay(recordedTimes,lastDelay);
					tripUpdate.addStopTimeUpdate(recordedTimes);
				}else if (posinfo.getMessagetype() == Type.OFFROUTE){
					StopTimeUpdate.Builder noData = StopTimeUpdate.newBuilder();
					noData.setStopSequence(pt.getPointorder());
					noData.setStopId(pt.getPointref().toString());
					noData.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
					tripUpdate.addStopTimeUpdate(noData);
					if (!hasMutations())
						break;
				}
			}
			this.posinfo = posinfo;
			tripUpdate = filter(tripUpdate);
			if (tripUpdate.getStopTimeUpdateCount() > 0)
				return tripUpdate;
		default:
			break;
		}
		int passageseq = 0;
		int elapsedtime = 0;
		int lastDelay = Integer.MIN_VALUE;
		for (int i = 0; i < timedemandgroup.getPoints().size(); i++) {
			TimeDemandGroupPoint tpt = timedemandgroup.getPoints().get(i);
			JourneyPatternPoint pt = journeypattern.getPoint(tpt.pointorder);
			if (pt.getOperatorpointref().equals(posinfo.getUserstopcode())) {
				if (posinfo.getPassagesequencenumber() - passageseq > 0) {
					passageseq++; // Userstop equal but posinfo relates to n-th
					// passing
				} else {
					elapsedtime = tpt.getTotaldrivetime()+tpt.getStopwaittime();
					passed = false;
					switch (posinfo.getMessagetype()) {
					case ARRIVAL:
						if (RECORD_TIMES)
							realizedArrivals.put(pt.getPointorder(), posinfo.getTimestamp());
						long targetArrivalTime = getDepartureEpoch()+tpt.getTotaldrivetime();
						int newPunctuality = (int)(posinfo.getTimestamp()-targetArrivalTime);
						if (newPunctuality > -60 && newPunctuality - punctuality < 600){
							punctuality = newPunctuality;
						}
						if ((pt.isWaitpoint() || i == 0)	&& punctuality < 0)
							punctuality = 0;
						break;
					case DEPARTURE:
						long targetDepartureTime = getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
						newPunctuality = (int)(posinfo.getTimestamp()-targetDepartureTime);
						if (RECORD_TIMES && (!pt.isWaitpoint() || newPunctuality > 0))
							realizedDepartures.put(pt.getPointorder(), posinfo.getTimestamp());
						if ((pt.isWaitpoint() || i == 0)	&& punctuality < 0)
							punctuality = 0;
						break;
					case ONSTOP:
						if ((pt.isWaitpoint() || i == 0)	&& punctuality < 0)
							punctuality = 0;
						targetDepartureTime = getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
						newPunctuality = (int)(posinfo.getTimestamp()-targetDepartureTime);
						if (newPunctuality > -30 && newPunctuality - punctuality < 600){
							punctuality = newPunctuality;
						}

						break;
					default:
						break;
					}
				}
				StopTimeUpdate.Builder recorded = recordedTimes(tpt,pt,lastDelay);
				if (recorded != null){
					lastDelay = lastDelay(recorded,lastDelay);
					tripUpdate.addStopTimeUpdate(recorded);
				}
			} else if (!passed) { //Stops not visted by the vehicle
				StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
				stopTimeUpdate.setStopSequence(tpt.getPointorder());
				stopTimeUpdate.setStopId(pt.getPointref().toString());
				stopTimeUpdate.setArrival(stopTimeEventArrival(tpt,pt,punctuality));
				boolean stopcanceled = isCanceled;
				boolean destChanged = false;
				if (mutations.containsKey(tpt.getPointorder())){ // Check if mutation exists with cancel
					for (Mutation m : mutations.get(tpt.getPointorder())){
						if (m.getMutationtype() == MutationType.SHORTEN){
							stopcanceled = true;
						}
						if (m.getMutationtype() == MutationType.CHANGEDESTINATION && !destChanged){
							destChanged = true;
							String destination = m.getDestinationname50();
							if (destination == null){
								destination = m.getDestinationname16();
							}
							if (destination == null){
								destination = m.getDestinationdisplay16();
							}
							if (destination != null){
								OVapiStopTimeUpdate.Builder ovapiStopTimeUpdate = OVapiStopTimeUpdate.newBuilder();
								ovapiStopTimeUpdate.setStopHeadsign(destination);
								stopTimeUpdate.setExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate, ovapiStopTimeUpdate.build());
							}
						}
					}
				}
				stopTimeUpdate.setScheduleRelationship(
						stopcanceled ? StopTimeUpdate.ScheduleRelationship.SKIPPED
								: StopTimeUpdate.ScheduleRelationship.SCHEDULED);
				if (pt.isWaitpoint() && punctuality < 0)
					punctuality = 0;
				if (tpt.getStopwaittime() != 0 && punctuality > 0) { //Minimize delay by cutting into dwells
					int stopwaittime = tpt.getStopwaittime();
					if (stopwaittime > 20 ) {
						punctuality -= Math.max(0, stopwaittime - 20);
						punctuality = Math.max(0, punctuality);
					}
				}
				stopTimeUpdate.setDeparture(stopTimeEventDeparture(tpt,pt,punctuality));
				if (pt.isScheduled()){
					tripUpdate.addStopTimeUpdate(stopTimeUpdate);
				}
				punctuality = stopTimeUpdate.getDeparture().getDelay();
				if (i+1 < timedemandgroup.getPoints().size()){
					TimeDemandGroupPoint ntpt = timedemandgroup.getPoints().get(i+1);
					JourneyPatternPoint npt = journeypattern.getPoint(ntpt.pointorder);
					int distanceToNext = npt.getDistancefromstartroute() - pt.getDistancefromstartroute();
					int secondsToNext = ntpt.getTotaldrivetime() - (tpt.getTotaldrivetime()+tpt.getStopwaittime());
					int speed = DEFAULT_SPEED;
					if (distanceToNext > 10000) {
						speed = LONGHAUL_SPEED;
					} else if (distanceToNext < 1000) {
						speed = SHORTHAUL_SPEED;
					}
					int fastest = distanceToNext / speed;
					if ((punctuality > 0 || secondsToNext < fastest) && i != timedemandgroup.getPoints().size() - 1) {
						punctuality -= (secondsToNext - fastest);
						if (punctuality < 0) {
							punctuality = 0;
						}
					} else if (punctuality < 0 && i != timedemandgroup.getPoints().size() - 1) {
						punctuality = decayeddelay(punctuality,
								tpt.getTotaldrivetime() - elapsedtime);
					}
					if (Math.abs(punctuality) < PUNCTUALITY_FLOOR) {
						punctuality = 0;
					}
				}
			}else{ //JourneyPatternPoint has been passed.
				StopTimeUpdate.Builder recorded = recordedTimes(tpt,pt,lastDelay);
				if (recorded != null){
					lastDelay = lastDelay(recorded,lastDelay);
					tripUpdate.addStopTimeUpdate(recorded);
				}
			}
		}
		this.posinfo = posinfo;
		tripUpdate = filter(tripUpdate);
		if (tripUpdate.getStopTimeUpdateCount() > 0)
			return tripUpdate;
		else
			return null;
	}

	public int decayeddelay(int delay, int elapsedtime) {
		if (delay == 0)
			return 0;
		double vlamba = 1.0 / 500.0;
		double decay = Math.exp(-vlamba * elapsedtime);
		int decayeddelay = (int) (decay * delay);
		return decayeddelay;

	}

	private void parseMutateJourney(Long timestamp, Mutation m) {
		switch (m.getMutationtype()) {
		case CANCEL:
			isCanceled = true;
			break;
		case RECOVER:
			isCanceled = false;
			break;
		default:
			break;

		}
	}

	private JourneyPatternPoint getJourneyStop (String userstopcode,int passageSequencenumber){
		for (int i = 0; i < timedemandgroup.getPoints().size(); i++) {
			TimeDemandGroupPoint tpt = timedemandgroup.getPoints().get(i);
			JourneyPatternPoint pt = journeypattern.getPoint(tpt.pointorder);
			if (pt.getOperatorpointref().equals(userstopcode)){
				if (passageSequencenumber > 0){
					passageSequencenumber--;
				}else{
					return pt;
				}
			}
		}
		return null;
	}

	private void parseMutateJourneyStop(Long timestamp, Mutation m)
			throws StopNotFoundException {
		JourneyPatternPoint pst = getJourneyStop(m.getUserstopcode(),m.getPassagesequencenumber());
		if (pst == null) {
			throw new StopNotFoundException(m.toString());
		}
		if (!mutations.containsKey(pst.getPointorder()))
			mutations.put(pst.getPointorder(), new ArrayList<Mutation>());
		switch (m.getMutationtype()) {
		case CHANGEDESTINATION:
		case CHANGEPASSTIMES:
		case LAG:
		case RECOVER:
		case CANCEL:
		case SHORTEN:
			mutations.get(pst.getPointorder()).add(m);
			break;
		default:
			_log.info("Unknown mutationtype {}",m);
			break;
		}
	}

	public TripUpdate.Builder update(ArrayList<KV17cvlinfo> cvlinfos) {
		long timestamp = 0;
		if (cvlinfos.size() == 0){
			return null;
		}
		mutations.clear();
		for (KV17cvlinfo cvlinfo : cvlinfos) {
			for (Mutation mut : cvlinfo.getMutations()) {
				try {
					timestamp = Math.max(timestamp, cvlinfo.getTimestamp());
					switch (mut.getMessagetype()) {
					case KV17MUTATEJOURNEY:
						parseMutateJourney(cvlinfo.getTimestamp(), mut);
						continue;
					case KV17MUTATEJOURNEYSTOP:
						parseMutateJourneyStop(cvlinfo.getTimestamp(), mut);
						continue;
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
		if (posinfo != null && posinfoAge < 120){
			TripUpdate.Builder timeUpdate = updateTimes(posinfo);
			timeUpdate.setTimestamp(timestamp);
			return timeUpdate;
		}else{
			KV6posinfo posinfo = new KV6posinfo();
			posinfo.setMessagetype(Type.DELAY); //Fake KV6posinfo to get things moving
			posinfo.setPunctuality(0);
			posinfo.setTimestamp(timestamp);
			return updateTimes(posinfo);
		}
	}

	public TripUpdate.Builder update(KV6posinfo posinfo) throws StopNotFoundException,UnknownKV6PosinfoType, TooEarlyException, TooOldException {
		long currentTime = Utils.currentTimeSecs();
		if (posinfo.getTimestamp()<currentTime-120){
			throw new TooOldException(posinfo.toString());
		}
		long departureTime = getDepartureEpoch();
		if (currentTime < departureTime){
			int timeDeltaSeconds = (int)(departureTime-Utils.currentTimeSecs());
			if (timeDeltaSeconds>=3600){
				switch(posinfo.getMessagetype()){
				case INIT:
				case ARRIVAL:
				case ONSTOP:
				case DELAY:
					break;
				default:
					throw new TooEarlyException(posinfo.toString());
				}
			}
		}
		if (posinfo.getUserstopcode() != null
				&& !journeypattern.contains(posinfo.getUserstopcode())) {
			throw new StopNotFoundException(posinfo.toString());
		}
		return updateTimes(posinfo);
	}
}

