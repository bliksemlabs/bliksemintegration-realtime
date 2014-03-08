package nl.ovapi.arnu;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopKind;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopType;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeIncrementalUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;

/**
 * Keeps collection of journeys with the same trainnumber in different blocks
 * @author Thomas Koch
 */
public class TrainProcessor {
	private static final Logger _log = LoggerFactory.getLogger(TrainProcessor.class);

	private List<JourneyProcessor> _processors;
	
	private TrainProcessor(){
		_processors = new ArrayList<JourneyProcessor>();
	}
	
	/**
	 * @param otherTrainProcessor 
	 * @return whether both TrainProcessors do not share a single station.
	 */
	
	public boolean isDisjunct(TrainProcessor otherTrainProcessor){
	    ArrayList<String> pathLeft = new ArrayList<String>();
	    for (JourneyPatternPoint pt : longestJourney().getJourneypattern().getPoints()){
			String stationCode = JourneyProcessor.stationCode(pt);
			pathLeft.add(stationCode);
	    }
	    for (JourneyPatternPoint pt : otherTrainProcessor.longestJourney().getJourneypattern().getPoints()){
			String stationCode = JourneyProcessor.stationCode(pt);
			if (pathLeft.contains(stationCode)){
				return false;
			}
	    }
	    return true;
	}
	
	private long startEpoch(){
		long res = Long.MAX_VALUE;
		for (JourneyProcessor jp : _processors){
			res = Math.min(jp.getJourney().getDepartureEpoch(),res);
		}
		return res;
	}
	
	public static Integer orginalTrainNumber(String trainCode){
		try{
			int trainNumber = Integer.parseInt(trainCode);
			if (trainNumber >= 300000 && trainNumber < 310000){
				return trainNumber - 300000;
			}else if (trainNumber >= 310000 && trainNumber < 320000){
				return trainNumber - 310000;
			}else if (trainNumber >= 320000 && trainNumber < 330000){
				return trainNumber - 320000;
			}else if (trainNumber >= 330000 && trainNumber < 340000){
				return trainNumber - 330000;
			}else if (trainNumber >= 340000 && trainNumber < 350000){
				return trainNumber - 340000;
			}else if (trainNumber >= 350000 && trainNumber < 360000){
				return trainNumber - 350000;
			}else if (trainNumber >= 360000 && trainNumber < 370000){
				return trainNumber - 360000;
			}else{
				return trainNumber;
			}
		}catch (Exception e){
			_log.error("Can't parse trainCode {}",trainCode);
			e.printStackTrace();
			return null;
		}
	}

	
	public TrainProcessor(@NonNull List<Journey> journeys){
		if (journeys.size() == 0){
			throw new IllegalArgumentException("No journeys given");
		}
		_processors = new ArrayList<JourneyProcessor>(journeys.size());
		for (Journey j : journeys){
			_processors.add(new JourneyProcessor(j));
		}
	}
	
	/**
	 * @return the route_id this train is a part of;
	 */
	
	public Long getRouteId(){
		// We're definitely should assume that all (segments) of this train journey are part of the same (GTFS) route
		return _processors.get(0).getJourney().getRouteId();
	}
	
	/**
	 * @param routeId will be set to all blocks in this train journey
	 */
	
	public void setRouteId(@NonNull Long routeId){
		for (JourneyProcessor jp : _processors){
			jp.getJourney().setRouteId(routeId);
		}
	}
	
	/**
	 * @return the journey which visits the most stations, assumed to be the journey that describes the entire trainpath
	 */
	private Journey longestJourney(){
		int maxLength = -1;
		Journey longestJourney = null;
		for (JourneyProcessor j : this._processors){
			if (j.getJourney().getJourneypattern().getPoints().size() > maxLength){
				maxLength = j.getJourney().getJourneypattern().getPoints().size();
				longestJourney = j.getJourney();
			}
		}
		return longestJourney;
	}
	
	public void changeService(@NonNull RIDservice ridService,ServiceInfoServiceType info) throws ParseException{
	    ArrayList<String> plannedPath = new ArrayList<String>();
	    for (JourneyPatternPoint pt : longestJourney().getJourneypattern().getPoints()){
			String stationCode = JourneyProcessor.stationCode(pt);
			plannedPath.add(stationCode);
	    }
	    if (_processors.size() > 1){
	    	_log.error("Journey path change not supported for multiple blocks...");
	    	return;
	    }
	    String lastStation =null;
		for (int i = 0; i < info.getStopList().getStop().size(); i++){
			ServiceInfoStopType s = info.getStopList().getStop().get(i);
			if ((s.getArrival() == null && s.getDeparture() == null ||
					s.getStopType() == ServiceInfoStopKind.CANCELLED_STOP || 
					s.getStopType() == ServiceInfoStopKind.DIVERTED_STOP)){
				continue;
			}
			if (!plannedPath.contains(s.getStopCode().toLowerCase())){
		    	_log.error("Add station {} ",s.getStopCode()+" after "+lastStation);
		    	for (JourneyProcessor jp : _processors){
		    		jp.addStoppoint(ridService,s, lastStation);
		    	}
			}
			lastStation = s.getStopCode().toLowerCase();
		}
	}
	
	public static TrainProcessor fromArnu(@NonNull RIDservice ridService,@NonNull ServiceInfoServiceType info){
		TrainProcessor p = new TrainProcessor();
		p._processors.add(JourneyProcessor.fromArnu(ridService, info));
		return p;
	}
		
	public GtfsRealtimeIncrementalUpdate process(ServiceInfoServiceType info){
		GtfsRealtimeIncrementalUpdate update = new GtfsRealtimeIncrementalUpdate();
		for (JourneyProcessor p : _processors){
			FeedEntity.Builder entity = FeedEntity.newBuilder();
			entity.setTripUpdate(p.process(info));
			entity.setId(p.getJourney().getId()+"");
			update.addUpdatedEntity(entity.build());
		}
		return update;
	}
}
