package nl.ovapi.arnu;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.gtfsrt.services.ARNUritInfoToGtfsRealTimeServices;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
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

public class JourneyProcessor {
	@Getter private Journey journey;
	private Map<String,Patch> patches;
	private static final Logger _log = LoggerFactory.getLogger(ARNUritInfoToGtfsRealTimeServices.class);
	private static class Patch {
		Long eta,etd;
		Integer arrivalDelay,departureDelay;
		Boolean canceled; 
	}

	public JourneyProcessor(@NonNull Journey j){
		journey = j;
		patches = Maps.newHashMap();
	}
	
	/**
	 * 
	 * @param stationCode NS stationcode such as lls, asd,asdz etc.
	 * @return whether this journey contains station of stationCode
	 */
	
	public boolean containsStation(String stationCode){
	    for (JourneyPatternPoint pt : journey.getJourneypattern().getPoints()){
	    	if (stationCode.equals(stationCode)){
	    		return true;
	    	}
	    }
	    return false;
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
					p.canceled = true;
					break;
				case DIVERTED_STOP:	
					_log.debug("Divertion not supported {}",info);
					break;
				case SPLIT_STOP:
					_log.debug("Split stop not supported {}",info);
					break;
				default:
					break;
				}
			}else{
				//This also sets passage-stations as canceled, but that is assumed to be a non-issue
				p.canceled = (station.getArrival() == null && station.getDeparture() == null);
			}
			if (station.getArrivalTimeDelay() != null){
				p.arrivalDelay = Utils.toSeconds(station.getArrivalTimeDelay());
				Calendar c = (Calendar) station.getArrival().toGregorianCalendar().clone();
				station.getArrivalTimeDelay().addTo(c);
				p.eta = c.getTimeInMillis()/1000; // in Seconds since 1970
			}else if (station.getArrival() != null){
				p.eta = station.getArrival().toGregorianCalendar().getTimeInMillis()/1000; // in Seconds since 1970
			}

			if (station.getDepartureTimeDelay() != null){
				p.departureDelay = Utils.toSeconds(station.getDepartureTimeDelay());
				Calendar c = (Calendar) station.getDeparture().toGregorianCalendar().clone();
				station.getDepartureTimeDelay().addTo(c);
				p.etd = c.getTimeInMillis()/1000; // in Seconds since 1970
			}else if (station.getDeparture() != null){
				p.etd = station.getDeparture().toGregorianCalendar().getTimeInMillis()/1000; // in Seconds since 1970
			}
			patches.put(stationCode, p);
		}
	}

	public TripUpdate.Builder process(@NonNull ServiceInfoServiceType info){
		updatePatches(info);
		switch (info.getServiceType()){
		case NORMAL_SERVICE:
		case NEW_SERVICE:
		case CANCELLED_SERVICE:
			break;
		case DIVERTED_SERVICE:
		case EXTENDED_SERVICE:
		case SCHEDULE_CHANGED_SERVICE:
		case SPLIT_SERVICE:
			_log.debug("Unsupported serviceType {}",info);
			break;
		default:
			break;

		}
		return buildTripUpdate();
	}

	private TripUpdate.Builder buildTripUpdate(){
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
				stop.setScheduleRelationship(p.canceled ? ScheduleRelationship.SKIPPED :
					ScheduleRelationship.SCHEDULED);
				if (p.canceled){
					stopsCanceled++; //Signal that there was a cancellation along the journey
				}
			}else{
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
			if (i != 0 && p.eta != null){
				StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
				arrival.setDelay(p.arrivalDelay == null ? 0 : p.arrivalDelay);
				arrival.setTime(p.eta); //In seconds since 1970 
				stop.setArrival(arrival);
			}
			if (i != journey.getJourneypattern().getPoints().size()-1 && p.etd != null){
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
		return trip;
	}
}
