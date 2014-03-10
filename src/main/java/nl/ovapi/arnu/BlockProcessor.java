package nl.ovapi.arnu;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import lombok.Getter;
import lombok.NonNull;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.gtfsrt.services.ARNUritInfoToGtfsRealTimeServices;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.Block;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoKind;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopKind;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship;

/**
 * Logical unit that translates ARNU updates into GTFS-RT updates
 * @author Thomas Koch
 *
 */

public class BlockProcessor {
	@Getter private Block block;
	private Map<String,Patch> patches;
	private static final Logger _log = LoggerFactory.getLogger(ARNUritInfoToGtfsRealTimeServices.class);
	private static class Patch {
		Long eta,etd;
		Integer arrivalDelay,departureDelay;
		boolean canceled; 
	}

	public BlockProcessor(@NonNull Block b){
		block = b;
		patches = Maps.newHashMap();
	}

	/**
	 * 
	 * @param stationCode NS stationcode such as lls, asd,asdz etc.
	 * @return whether this journey contains station of stationCode
	 */

	public boolean containsStation(String stationCode){
		for (Journey journey : block.getSegments()){
			for (JourneyPatternPoint pt : journey.getJourneypattern().getPoints()){
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

	public void addStoppoint(RIDservice ridService,ServiceInfoStopType stop, @NonNull String afterStation) throws ParseException{
		if (stop.getArrival() == null && stop.getDeparture() == null){
			throw new IllegalArgumentException("No times for stop");
		}
		for (Journey journey : block.getSegments()){
			for (int i = 0;i < journey.getJourneypattern().getPoints().size();i++){
				JourneyPatternPoint pt = journey.getJourneypattern().getPoints().get(i);
				String stationCode = stationCode(pt);
				_log.error("Adding station, checking {} ",stationCode);
				if (stationCode.equals(afterStation)){
					_log.error("Found match at {} adding stop ",stationCode);
					JourneyPatternPoint newJpt = new JourneyPatternPoint();
					newJpt.setAdded(true);
					if (journey.getJourneypattern().getPoint(pt.getPointorder()+1) != null){
						throw new IllegalArgumentException("Duplicate pointorder "+stop.getStopCode()+" "+stop.getStopServiceCode());
					}
					newJpt.setPointorder(pt.getPointorder()+1);
					newJpt.setScheduled(true);
					newJpt.setWaitpoint(true);
					newJpt.setOperatorpointref(String.format("%s:0", stop.getStopCode().toLowerCase()));
					Long id = ridService.getRailStation(stop.getStopCode().toLowerCase());
					if (id == null){
						_log.error("PointId for station {} not found",stop.getStopCode());
					}else{
						newJpt.setPointref(id);
						journey.getJourneypattern().getPoints().add(i+1,newJpt);
					}
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					Date date = df.parse(journey.getOperatingDay());
					XMLGregorianCalendar time = stop.getArrival() != null ? stop.getArrival() : stop.getDeparture();
					int totaldrivetime = (int) ((time.toGregorianCalendar().getTimeInMillis() - date.getTime())/1000);
					int stopwaittime = 0;
					if (stop.getDeparture() != null){
						stopwaittime = (int) ((stop.getDeparture().toGregorianCalendar().getTimeInMillis() -
								stop.getArrival().toGregorianCalendar().getTimeInMillis())/1000);
					}
					TimeDemandGroupPoint tpt = new TimeDemandGroup.TimeDemandGroupPoint();
					tpt.setPointorder(pt.getPointorder()+1);
					tpt.setStopwaittime(stopwaittime);
					tpt.setTotaldrivetime(totaldrivetime);
					journey.getTimedemandgroup().getPoints().add(i+1, tpt);
					_log.error("Stop added ",stationCode);
					_log.error("JourneyPattern {}",journey.getJourneypattern());
					_log.error("TimeGroup {}",journey.getTimedemandgroup());
					if (journey.getJourneypattern().getPoints().size() != journey.getTimedemandgroup().getPoints().size()){
						throw new IllegalArgumentException("Adding point failed: Size of timedemandgroup and journeypattern do not match "+journey);
					}				
					_log.error("Service {} Stop {} added sucessfully for ",stop.getStopServiceCode(),stop.getStopCode());
					return;
				}
			}
		}
		_log.error("Station "+stop.getStopCode()+" after "+afterStation+" not added");
	}

	public static String stationCode(JourneyPattern.JourneyPatternPoint point){
		return point.getOperatorpointref().split(":")[0];
	}

	private static TimeDemandGroup timePatternFromArnu(Journey j,ServiceInfoServiceType info){
		TimeDemandGroup tp = new TimeDemandGroup();
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
				TimeDemandGroup.TimeDemandGroupPoint pt = new TimeDemandGroup.TimeDemandGroupPoint();
				pt.setPointorder(i);
				pt.setStopwaittime(0);
				pt.setTotaldrivetime(0);
				tp.add(pt);
			}else{
				Calendar c = s.getArrival().toGregorianCalendar();
				//SEconds since midnight
				int time = secondsSinceMidnight(c);
				TimeDemandGroup.TimeDemandGroupPoint pt = new TimeDemandGroup.TimeDemandGroupPoint();
				pt.setTotaldrivetime(time-departuretime);
				if (s.getDeparture() != null){
					c = s.getDeparture().toGregorianCalendar();
					int depTime = secondsSinceMidnight(c);
					pt.setStopwaittime(depTime-time);
				}else{
					pt.setStopwaittime(0);
				}
				tp.add(pt);
			}
		}
		return tp;
	}	

	private static JourneyPattern patternFromArnu(RIDservice ridService,ServiceInfoServiceType info){
		JourneyPattern jp = new JourneyPattern();
		try{
			jp.setDirectiontype(Integer.parseInt(info.getServiceCode())%2 == 0 ? 2 :1);
		}catch (Exception e){}
		for (int i = 0; i < info.getStopList().getStop().size(); i++){
			ServiceInfoStopType s = info.getStopList().getStop().get(i);
			if (s.getArrival() == null && s.getDeparture() == null){
				continue; //Train does not stop at this station;
			}
			JourneyPattern.JourneyPatternPoint pt = new JourneyPattern.JourneyPatternPoint();
			pt.setPointorder((i+1)*10);
			pt.setScheduled(true);
			pt.setWaitpoint(true);
			pt.setOperatorpointref(String.format("%s:0", s.getStopCode().toLowerCase()));
			Long id = ridService.getRailStation(s.getStopCode().toLowerCase());
			if (id == null){
				_log.error("PointId for station {} not found",s.getStopCode());
			}else{
				pt.setPointref(id);
				jp.add(pt);
			}
		}
		return jp;
	}

	public static int secondsSinceMidnight(Calendar c){
		return c.get(Calendar.HOUR_OF_DAY)*60*60+c.get(Calendar.MINUTE)*60+c.get(Calendar.SECOND);
	}

	public static BlockProcessor fromArnu(@NonNull RIDservice ridService,@NonNull ServiceInfoServiceType info){
		try{
			Journey j = new Journey();
			j.setAdded(true);
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			j.setPrivateCode(String.format("%s:IFF:%s:%s",df.format(new Date()),info.getTransportModeCode(),info.getServiceCode()));
			j.setId(j.getPrivateCode());
			j.setJourneypattern(patternFromArnu(ridService,info));
			j.setTimedemandgroup(timePatternFromArnu(j,info));
			if (j.getJourneypattern().getPoints().size() != j.getTimedemandgroup().getPoints().size()){
				throw new IllegalArgumentException("Size of timedemandgroup and journeypattern do not match");
			}
			Block b = new Block(null);
			b.addJourney(j);
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
		for (Journey j : block.getSegments()){
			j.setRouteId(routeId);
		}
	}

	private void updatePatches(ServiceInfoServiceType info){
		if (info.getStopList() == null || info.getStopList().getStop() == null){
			return;
		}

		for (ServiceInfoStopType station : info.getStopList().getStop()){
			String stationCode = station.getStopCode().toLowerCase();
			Patch p = new Patch();
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
	}

	public List<TripUpdate.Builder> process(@NonNull ServiceInfoServiceType info){
		patches.clear(); //TODO Figure out whether ARNU updates are replacing each other.
		updatePatches(info);
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
		return buildTripUpdate(info);
	}

	private List<TripUpdate.Builder> buildTripUpdate(@NonNull ServiceInfoServiceType info){
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
				if (p != null){
					if (p.canceled || jp.isSkipped()){
						stop.setScheduleRelationship(ScheduleRelationship.SKIPPED);
						stopsCanceled++; //Signal that there was a cancellation along the journey
					}else if (jp.isAdded()){
						stop.setScheduleRelationship(ScheduleRelationship.ADDED);
					}else{
						stop.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
					}
				}else if (jp.isSkipped() || (info.getServiceType() != null && info.getServiceType() == ServiceInfoKind.EXTENDED_SERVICE)){
					stop.setScheduleRelationship(ScheduleRelationship.SKIPPED);
					stopsCanceled++;
				}else if (!jp.isSkipped()){
					_log.error("No Realtime stop info for {} {}",jp.getOperatorpointref(),info);
					if (i != 0){
						StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
						arrival.setDelay(0);
						arrival.setTime(journey.getArrivalTime(jp.getPointorder())); //In seconds since 1970 
						stop.setArrival(arrival);
					}
					if (i != journey.getJourneypattern().getPoints().size()-1){
						StopTimeEvent.Builder departure = StopTimeEvent.newBuilder();
						departure.setDelay(0);
						departure.setTime(journey.getDepartureTime(jp.getPointorder())); //In seconds since 1970 
						stop.setArrival(departure);
					}
					continue;
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
				trip.addStopTimeUpdate(stop);
			}
			if (stopsCanceled > journey.getJourneypattern().getPoints().size()-2){
				//Journey now exists of a single stop or less
				trip.clearStopTimeUpdate();
				journey.setCanceled(true);
			}else{
				journey.setCanceled(false);
			}
			trip.setTrip(journey.tripDescriptor());
			updates.add(trip);
		}
		return updates;
	}
}
