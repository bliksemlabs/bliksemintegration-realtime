package nl.ovapi.arnu;

import java.util.Calendar;
import java.util.Map;

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
	private Journey _journey;
	private Map<String,Patch> patches;
	private static final Logger _log = LoggerFactory.getLogger(ARNUritInfoToGtfsRealTimeServices.class);
	private static class Patch {
		Long eta,etd;
		Integer arrivalDelay,departureDelay;
		Boolean canceled; 
	}

	public JourneyProcessor(@NonNull Journey j){
		_journey = j;
		patches = Maps.newHashMap();
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
				p.canceled = false;
			}
			if (station.getArrivalTimeDelay() != null){
				p.arrivalDelay = Utils.toSeconds(station.getArrivalTimeDelay());
				Calendar c = (Calendar) station.getArrival().toGregorianCalendar().clone();
				station.getArrivalTimeDelay().addTo(c);
				p.eta = c.getTimeInMillis();
			}else if (station.getArrival() != null){
				p.eta = station.getArrival().toGregorianCalendar().getTimeInMillis();
			}

			if (station.getDepartureTimeDelay() != null){
				p.departureDelay = Utils.toSeconds(station.getDepartureTimeDelay());
				Calendar c = (Calendar) station.getDeparture().toGregorianCalendar().clone();
				station.getDepartureTimeDelay().addTo(c);
				p.etd = c.getTimeInMillis();
			}else if (station.getDeparture() != null){
				p.etd = station.getDeparture().toGregorianCalendar().getTimeInMillis();
			}
			patches.put(stationCode, p);
		}
	}

	public void process(@NonNull ServiceInfoServiceType info){
		updatePatches(info);
		switch (info.getServiceType()){
		case NORMAL_SERVICE:
			break;
		case CANCELLED_SERVICE:
		case DIVERTED_SERVICE:
		case EXTENDED_SERVICE:
		case NEW_SERVICE:
		case SCHEDULE_CHANGED_SERVICE:
		case SPLIT_SERVICE:
			_log.debug("Unsupported serviceType {}",info);
			break;
		default:
			break;

		}
		System.out.println(buildTripUpdate().build());
	}
	
	private TripUpdate.Builder buildTripUpdate(){
		TripUpdate.Builder trip = TripUpdate.newBuilder();
		//Keep track whether all stations are canceled along the journey
		Boolean completelyCanceled = null; 
		for (int i = 0;i < _journey.getJourneypattern().getPoints().size();i++){
			JourneyPatternPoint jp = _journey.getJourneypattern().getPoints().get(i);
			StopTimeUpdate.Builder stop = StopTimeUpdate.newBuilder();
			stop.setStopId(jp.getPointref()+"");
			stop.setStopSequence(jp.getPointorder());
			String stationCode = jp.getOperatorpointref().split(":")[0].toLowerCase();
			Patch p = patches.get(stationCode);
			if (p != null){
				stop.setScheduleRelationship(p.canceled ? ScheduleRelationship.SKIPPED :
						ScheduleRelationship.SCHEDULED);
				if (p.canceled){
					completelyCanceled = true; //Signal that there was a cancellation along the journey
				}else if (completelyCanceled != null){
					completelyCanceled = false; //Signal that there was a station that was not cancelled
				}
			}else{
				completelyCanceled = false; //Signal that there was a station that was not cancelled
				//TODO set static times or skip updates?
				continue;
			}
			if (i != 0){
				StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
				arrival.setDelay(p.arrivalDelay == null ? 0 : p.arrivalDelay);
				arrival.setTime(p.eta);
				stop.setArrival(arrival);
			}
			if (i != _journey.getJourneypattern().getPoints().size()-1){
				StopTimeEvent.Builder departure = StopTimeEvent.newBuilder();
				departure.setDelay(p.departureDelay == null ? 0 : p.departureDelay);
				departure.setTime(p.etd);
				stop.setDeparture(departure);
			}
			trip.addStopTimeUpdate(stop);
		}
		if (completelyCanceled != null && completelyCanceled){
			trip.clearStopTimeUpdate();
			_journey.setCanceled(true);
		}else{
			_journey.setCanceled(false);
		}
		trip.setTrip(_journey.tripDescriptor());
		return trip;
	}
}
