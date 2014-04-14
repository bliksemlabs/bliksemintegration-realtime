package nl.ovapi.arnu;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.Getter;
import lombok.NonNull;
import lombok.Synchronized;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.gtfsrt.services.ARNUritInfoToGtfsRealTimeServices;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.Block;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopKind;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtimeNYCT;
import com.google.transit.realtime.GtfsRealtimeNYCT.NyctStopTimeUpdate;

/**
 * Logical unit that translates ARNU updates into GTFS-RT updates
 * @author Thomas Koch
 *
 */

public class BlockProcessor {
	@Getter private Block block;
	private final Object writeLock = new Object();
	private static final Logger _log = LoggerFactory.getLogger(ARNUritInfoToGtfsRealTimeServices.class);
	private static class Patch {
		Long eta,etd;
		Integer arrivalDelay,departureDelay;
		String serviceCode,departurePlatform,actualDeparturePlatform,arrivalPlatform,actualArrivalPlatform;
		boolean canceled; 
	}

	public BlockProcessor(@NonNull Block b){
		block = b;
	}

	/**
	 * 
	 * @param stationCode NS stationcode such as lls, asd,asdz etc.
	 * @param serviceCode of train passing by
	 * @return whether this journey contains station of stationCode
	 */

	public boolean containsStation(String stationCode, String serviceCode){
		for (int i = 0; i < block.getSegments().size();i++){
			Journey journey = block.getSegments().get(i);
			for (int j = 0; j < journey.getJourneypattern().getPoints().size(); j++){
				boolean lastStop = (j == journey.getJourneypattern().getPoints().size()-1);
				String serviceCodeOfJourney = serviceCode(journey);
				//Either no privatecode (realtime added) or serviceCode matches
				boolean serviceCodeMatches = serviceCodeOfJourney == null ? true : (serviceCodeOfJourney.equals(serviceCode));
				if (!(lastStop || serviceCodeMatches)){
					continue; //Journeypattern point is not part of serviceCode nor at end of a segment
				}
				JourneyPatternPoint pt = journey.getJourneypattern().getPoints().get(j);
				if (stationCode.equals(stationCode(pt))){
					return true;
				}
			}
		}
		return false;
	}

	public Set<String> getStations(){
		HashSet<String> stations = new HashSet<String>();
		for (Journey journey : block.getSegments()){
			for (int i = 0;i < journey.getJourneypattern().getPoints().size();i++){
				JourneyPatternPoint pt = journey.getJourneypattern().getPoints().get(i);
				String stationCode = stationCode(pt);
				stations.add(stationCode);
			}
		}
		return stations;
	}

	@Synchronized("writeLock")
	public void changeOrigin(RIDservice ridService,ServiceInfoServiceType info, ServiceInfoStopType newOrigin, ServiceInfoStopType prev, ServiceInfoStopType next) throws ParseException{
		if (block.getSegments().size() > 1){
			_log.error("This is not easy {}",info); //TODO think about all the known unknown's
			return;
		}
		int segmentIndex = -1;
		Journey toEdit = null;
		Journey.Builder editor = null;
		for (Journey j : block.getSegments()){
			segmentIndex++;
			if (newOrigin.getStopServiceCode().equals(serviceCode(j))){
				toEdit = j;
				editor = j.edit();
				break;
			}
		}
		if (editor == null){
			_log.error("Block serviceCode does not match {}",info);
			return;
		}
		boolean isDisjunct = true;
		for (int i = 0; i < info.getStopList().getStop().size(); i++){
			ServiceInfoStopType stop = info.getStopList().getStop().get(i);
			if (!stop.getStopServiceCode().equals(newOrigin.getStopServiceCode())){
				continue;
			}
			if (containsStation(stop.getStopCode(), stop.getStopServiceCode())){
				isDisjunct = false;
				break;
			}
		}
		if (isDisjunct){
			_log.error("No matching stops {}",info); //TODO
			return;
		}
		long newEpoch = newOrigin.getDeparture().toGregorianCalendar().getTimeInMillis()/1000;
		int newDepartureTime = editor.getDeparturetime()-(int)(toEdit.getDepartureEpoch()-newEpoch);
		editor.setDeparturetime(newDepartureTime);

		//TODO This is a shortcut, it should also be possible to manually add points.
		//But that will come with a lot of nasty unknown sideeffects..
		editor.setJourneyPattern(patternFromArnu(ridService,info));
		editor.setTimeDemandGroup(timePatternFromArnu(editor,info));
		getBlock().getSegments().set(segmentIndex, editor.build());
	}

	public static String serviceCode(Journey j){
		String[] ids = j.getPrivateCode().split(":");
		//Either no privatecode (realtime added) or serviceCode matches
		if (ids.length == 0){
			return null;
		}
		return ids[ids.length-1];
	}

	private static Long getStationId(RIDservice ridService,ServiceInfoStopType stop){
		Long id = null;
		if (stop.getActualDeparturePlatform() != null){
			id = ridService.getRailStation(stop.getStopCode().toLowerCase(),stop.getActualDeparturePlatform());
		}
		if (id == null || stop.getDeparturePlatform() != null){
			id = ridService.getRailStation(stop.getStopCode().toLowerCase(),stop.getDeparturePlatform());
		}
		if (id != null){
			return id;
		}else{
			return ridService.getRailStation(stop.getStopCode().toLowerCase(),"0");
		}
	}

	private static String operatorPointRef(ServiceInfoStopType stop){
		if (stop.getActualDeparturePlatform() != null){
			return String.format("%s:%s", stop.getStopCode().toLowerCase(),stop.getActualDeparturePlatform());
		}
		if (stop.getDeparturePlatform() != null){
			return String.format("%s:%s", stop.getStopCode().toLowerCase(),stop.getDeparturePlatform());
		}
		return String.format("%s:0", stop.getStopCode().toLowerCase());
	}

	@Synchronized("writeLock")
	public void addStoppoint(RIDservice ridService,ServiceInfoStopType stop, @NonNull String afterStation) throws ParseException{
		if (stop.getArrival() == null && stop.getDeparture() == null){
			throw new IllegalArgumentException("No times for stop");
		}
		for (int j = 0; j < block.getSegments().size(); j++){
			Journey journey = block.getSegments().get(j);
			Journey.Builder builder = null;
			for (int i = 0;i < journey.getJourneypattern().getPoints().size();i++){
				JourneyPatternPoint pt = journey.getJourneypattern().getPoints().get(i);
				String stationCode = stationCode(pt);
				_log.error("Adding station, checking {} ",stationCode);
				if (stationCode.equals(afterStation)){
					builder = journey.edit();
					_log.error("Found match at {} adding stop ",stationCode);
					if (journey.getJourneypattern().getPoint(pt.getPointorder()+1) != null){
						throw new IllegalArgumentException("Duplicate pointorder "+stop.getStopCode()+" "+stop.getStopServiceCode());
					}
					JourneyPatternPoint.Builder newJpt = JourneyPatternPoint.newBuilder()
							.setIsAdded(true)
							.setPointOrder(pt.getPointorder()+1)
							.setIsScheduled(true)
							.setIsWaitpoint(true)
							.setOperatorPointRef(operatorPointRef(stop));
					Long id = getStationId(ridService,stop);
					JourneyPattern.Builder newPattern = journey.getJourneypattern().edit();
					if (id == null){
						_log.error("PointId for station {} not found",stop.getStopCode());
					}else{
						newJpt.setPointRef(id);
						newPattern.add(i+1,newJpt.build());
					}
					builder.setJourneyPattern(newPattern.build());
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					Date date = df.parse(journey.getOperatingDay());
					XMLGregorianCalendar time = stop.getArrival() != null ? stop.getArrival() : stop.getDeparture();
					int totaldrivetime = (int) ((time.toGregorianCalendar().getTimeInMillis() - date.getTime())/1000);
					int stopwaittime = 0;
					if (stop.getDeparture() != null){
						stopwaittime = (int) ((stop.getDeparture().toGregorianCalendar().getTimeInMillis() -
								stop.getArrival().toGregorianCalendar().getTimeInMillis())/1000);
					}
					TimeDemandGroup.Builder td = journey.getTimedemandgroup().edit();
					TimeDemandGroupPoint tpt = TimeDemandGroupPoint.newBuilder()
							.setPointOrder(pt.getPointorder()+1)
							.setStopWaitTime(stopwaittime)
							.setTotalDriveTime(totaldrivetime).build();
					td.add(i+1, tpt);
					builder.setTimeDemandGroup(td.build());
					_log.error("Stop added ",stationCode);
					_log.error("JourneyPattern {}",journey.getJourneypattern());
					_log.error("TimeGroup {}",journey.getTimedemandgroup());
					if (journey.getJourneypattern().getPoints().size() != journey.getTimedemandgroup().getPoints().size()){
						throw new IllegalArgumentException("Adding point failed: Size of timedemandgroup and journeypattern do not match "+journey);
					}				
					_log.error("Service {} Stop {} added sucessfully for ",stop.getStopServiceCode(),stop.getStopCode());
					if (builder != null){
						block.getSegments().set(j, builder.build());
					}
					return;
				}
			}
		}
		_log.error("Station "+stop.getStopCode()+" after "+afterStation+" not added");
	}

	public static String stationCode(JourneyPattern.JourneyPatternPoint point){
		return point.getOperatorpointref().split(":")[0];
	}

	private static TimeDemandGroup timePatternFromArnu(Journey.Builder j,ServiceInfoServiceType info){
		TimeDemandGroup.Builder tp = TimeDemandGroup.newBuilder();
		int departuretime = -1;
		for (int i = 0; i < info.getStopList().getStop().size(); i++){
			ServiceInfoStopType s = info.getStopList().getStop().get(i);
			if (s.getArrival() == null && s.getDeparture() == null){
				continue; //Train does not stop at this station;
			}
			if (departuretime == -1){
				Calendar c = s.getDeparture().toGregorianCalendar();
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				j.setOperatingDay(df.format(c.getTime()));
				//Seconds since midnight
				departuretime = secondsSinceMidnight(c);
				j.setDeparturetime(departuretime);
				TimeDemandGroup.TimeDemandGroupPoint pt = TimeDemandGroupPoint.newBuilder()
						.setPointOrder(i)
						.setStopWaitTime(0)
						.setTotalDriveTime(0).build();
				tp.add(pt);
			}else{
				Calendar c = s.getArrival() == null ? s.getDeparture().toGregorianCalendar() : 
					s.getArrival().toGregorianCalendar();
				//SEconds since midnight
				int time = secondsSinceMidnight(c);
				TimeDemandGroup.TimeDemandGroupPoint.Builder pt = TimeDemandGroupPoint.newBuilder()
						.setTotalDriveTime(time-departuretime)
						.setPointOrder(i);
				if (s.getDeparture() != null){
					c = s.getDeparture().toGregorianCalendar();
					int depTime = secondsSinceMidnight(c);
					pt.setStopWaitTime(depTime-time);
				}else{
					pt.setStopWaitTime(0);
				}
				tp.add(pt.build());
			}
		}
		return tp.build();
	}	

	private static JourneyPattern patternFromArnu(RIDservice ridService,ServiceInfoServiceType info){
		JourneyPattern.Builder jp = JourneyPattern.newBuilder();
		try{
			jp.setDirectionType(Integer.parseInt(info.getServiceCode())%2 == 0 ? 2 :1);
		}catch (Exception e){}
		for (int i = 0; i < info.getStopList().getStop().size(); i++){
			ServiceInfoStopType s = info.getStopList().getStop().get(i);
			if (s.getArrival() == null && s.getDeparture() == null){
				continue; //Train does not stop at this station;
			}
			JourneyPattern.JourneyPatternPoint.Builder pt = JourneyPatternPoint.newBuilder()
					.setPointOrder((i+1)*10)
					.setIsScheduled(true)
					.setIsWaitpoint(true)
					.setOperatorPointRef(operatorPointRef(s));
			Long id = getStationId(ridService,s);
			if (id == null){
				_log.error("PointId for station {} not found",s.getStopCode());
			}else{
				pt.setPointRef(id);
				jp.add(pt.build());
			}
		}
		return jp.build();
	}

	public static int secondsSinceMidnight(Calendar c){
		return c.get(Calendar.HOUR_OF_DAY)*60*60+c.get(Calendar.MINUTE)*60+c.get(Calendar.SECOND);
	}

	private final static SimpleDateFormat ISO_DATE = new SimpleDateFormat("yyyy-MM-dd");
	static {
		ISO_DATE.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));
	}

	public static BlockProcessor fromArnu(@NonNull RIDservice ridService,@NonNull ServiceInfoServiceType info){
		try{
			Journey.Builder j = Journey.newBuilder()
					.setIsAdded(true)
					.setPrivateCode(String.format("%s:IFF:%s:%s",ISO_DATE.format(new Date()),info.getTransportModeCode(),info.getServiceCode()))
					.setId(String.format("%s:IFF:%s:%s",ISO_DATE.format(new Date()),info.getTransportModeCode(),info.getServiceCode()))
					.setJourneyPattern(patternFromArnu(ridService,info));
			j.setTimeDemandGroup(timePatternFromArnu(j,info));
			j.setId(j.getPrivateCode());
			if (j.getJourneypattern().getPoints().size() != j.getTimedemandgroup().getPoints().size()){
				throw new IllegalArgumentException("Size of timedemandgroup and journeypattern do not match");
			}
			Block b = new Block(null);
			b.addJourney(j.build());
			return new BlockProcessor(b);
		}catch (Exception e){
			_log.error("Exception during Journey adding {}",info,e);
			throw e;
		} 
	}

	public String getId(){
		return block.getSegments().get(0).getId();
	}

	/**
	 * @param routeId will be set to all blocks in this train journey
	 */

	public void setRouteId(@NonNull Long routeId){
		for (int i = 0; i < block.getSegments().size();i++){
			block.getSegments().set(i, block.getSegments().get(i).edit().setRouteId(routeId).build());
		}
	}

	private Map<String, Patch> getPatches(ServiceInfoServiceType info){
		Map<String,Patch> patches = new HashMap<String, Patch>();
		if (info.getStopList() == null || info.getStopList().getStop() == null){
			return null;
		}
		for (ServiceInfoStopType station : info.getStopList().getStop()){
			String stationCode = station.getStopCode().toLowerCase();
			Patch p = new Patch();
			p.serviceCode = station.getStopServiceCode();
			p.actualArrivalPlatform = station.getActualArrivalPlatform();
			p.arrivalPlatform = station.getArrivalPlatform();
			p.actualDeparturePlatform = station.getActualDeparturePlatform();
			p.departurePlatform = station.getDeparturePlatform();
			if (station.getStopType() != null){
				switch (station.getStopType()){
				case CANCELLED_STOP:
				case DIVERTED_STOP:	
					p.canceled = true;
					break;
				case SPLIT_STOP:
					_log.debug("Split stop not supported {}",info);
					break;
				default:
					break;
				}
			}else{
				//This also sets passage-stations as canceled, but that is assumed to be a non-issue
				p.canceled = ((station.getArrival() == null && station.getDeparture() == null) ||
						station.getStopType() == ServiceInfoStopKind.CANCELLED_STOP || 
						station.getStopType() == ServiceInfoStopKind.DIVERTED_STOP);
			}
			if (station.getArrivalTimeDelay() != null){
				p.arrivalDelay = Utils.toSeconds(station.getArrivalTimeDelay());
				Calendar c = (Calendar) station.getArrival().toGregorianCalendar().clone();
				station.getArrivalTimeDelay().addTo(c);
				p.eta = (c.getTimeInMillis()/1000) + p.arrivalDelay; // in Seconds since 1970
			}else if (station.getArrival() != null){
				p.eta = station.getArrival().toGregorianCalendar().getTimeInMillis()/1000; // in Seconds since 1970
			}
			if (station.getDepartureTimeDelay() != null){
				p.departureDelay = Utils.toSeconds(station.getDepartureTimeDelay());
				Calendar c = (Calendar) station.getDeparture().toGregorianCalendar().clone();
				station.getDepartureTimeDelay().addTo(c);
				p.etd = (c.getTimeInMillis()/1000) + p.departureDelay; // in Seconds since 1970
			}else if (station.getDeparture() != null){
				p.etd = station.getDeparture().toGregorianCalendar().getTimeInMillis()/1000; // in Seconds since 1970
			}
			patches.put(stationCode, p);
		}
		return patches;
	}

	@Synchronized("writeLock")
	public List<TripUpdate.Builder> process(@NonNull ServiceInfoServiceType info){
		Map<String,Patch> patches = getPatches(info);
		switch (info.getServiceType()){
		case NORMAL_SERVICE:
		case NEW_SERVICE:
		case CANCELLED_SERVICE:
		case DIVERTED_SERVICE:
		case EXTENDED_SERVICE:
		case SCHEDULE_CHANGED_SERVICE:
			break;
		case SPLIT_SERVICE:
			_log.debug("Unsupported serviceType {}",info);
			break;
		default:
			break;

		}
		if (patches == null){
			return null;
		}
		return buildTripUpdate(patches,info);
	}

	private List<TripUpdate.Builder> buildTripUpdate(Map<String,Patch> patches,@NonNull ServiceInfoServiceType info){
		List<TripUpdate.Builder> updates = new ArrayList<TripUpdate.Builder>();
		for (Journey journey : block.getSegments()){
			TripUpdate.Builder trip = TripUpdate.newBuilder();
			//Keep track how many stations are canceled along the journey
			int stopsCanceled = 0; 
			for (int i = 0;i < journey.getJourneypattern().getPoints().size();i++){
				JourneyPatternPoint jp = journey.getJourneypattern().getPoints().get(i);
				StopTimeUpdate.Builder stop = StopTimeUpdate.newBuilder();
				stop.setStopId(jp.getPointref()+"");
				stop.setStopSequence(jp.getPointorder());
				String stationCode = jp.getOperatorpointref().split(":")[0].toLowerCase();
				Patch p = patches.get(stationCode);
				boolean lastStop = (i == journey.getJourneypattern().getPoints().size()-1);
				String serviceCode = serviceCode(journey);
				System.out.println("P" + journey.getPrivateCode() + " "+serviceCode + " " + p.serviceCode);
				//Either no privatecode (realtime added) or serviceCode matches
				boolean serviceCodeMatches = serviceCode == null ? true : (serviceCode.equals(p.serviceCode));
				if (p != null && (lastStop || serviceCodeMatches )){ //Either servicecode matches or last stop (which can involve a switch of servicecode)
					if (p.canceled || jp.isSkipped()){
						stop.setScheduleRelationship(ScheduleRelationship.SKIPPED);
						stopsCanceled++; //Signal that there was a cancellation along the journey
					}else if (jp.isAdded()){
						stop.setScheduleRelationship(ScheduleRelationship.ADDED);
					}else{
						stop.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
					}
					if (i != 0 && p.eta != null && !p.canceled){
						StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
						arrival.setDelay(p.arrivalDelay == null ? 0 : p.arrivalDelay);
						arrival.setTime(p.eta); //In seconds since 1970 
						stop.setArrival(arrival);
					}
					if (i != journey.getJourneypattern().getPoints().size()-1 && p.etd != null && !p.canceled){
						StopTimeEvent.Builder departure = StopTimeEvent.newBuilder();
						departure.setDelay(p.departureDelay == null ? 0 : p.departureDelay);
						departure.setTime(p.etd); //In seconds since 1970 
						stop.setDeparture(departure);
					}
					if (p.departurePlatform != null || p.actualDeparturePlatform != null){
						NyctStopTimeUpdate.Builder extension = NyctStopTimeUpdate.newBuilder();
						if (p.departurePlatform != null)
							extension.setScheduledTrack(p.departurePlatform);
						if (p.actualDeparturePlatform != null)
							extension.setActualTrack(p.actualDeparturePlatform);
						stop.setExtension(GtfsRealtimeNYCT.nyctStopTimeUpdate, extension.build());
					}
				}else{
					stop.setScheduleRelationship(ScheduleRelationship.SKIPPED);
					stopsCanceled++;
				}
				trip.addStopTimeUpdate(stop);
			}
			TripDescriptor.Builder tripDesc = journey.tripDescriptor();
			if (stopsCanceled > journey.getJourneypattern().getPoints().size()-2){
				//Journey now exists of a single stop or less
				trip.clearStopTimeUpdate();
				tripDesc.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);
			}
			trip.setTrip(tripDesc);
			updates.add(trip);
		}
		return updates;
	}
}
