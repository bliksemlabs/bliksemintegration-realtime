package nl.ovapi.arnu;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import lombok.NonNull;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.Block;
import nl.ovapi.rid.model.Journey;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopKind;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopType;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeIncrementalUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;

/**
 * Keeps collection of blocks starting with the same trainnumber in different blocks
 * @author Thomas Koch
 */
public class TrainProcessor {
	private static final Logger _log = LoggerFactory.getLogger(TrainProcessor.class);

	private List<BlockProcessor> _processors;

	private TrainProcessor(){
		_processors = new ArrayList<BlockProcessor>();
	}

	/**
	 * @param otherTrainProcessor 
	 * @return whether both TrainProcessors do not share a single station.
	 */

	public boolean isDisjoint(TrainProcessor otherTrainProcessor){
		HashSet<String> plannedLeft = new HashSet<String>();
		for (BlockProcessor p : _processors){
			plannedLeft.addAll(p.getStations());
		}

		HashSet<String> plannedRight = new HashSet<String>();
		for (BlockProcessor p : otherTrainProcessor._processors){
			plannedRight.addAll(p.getStations());
		}
		return Collections.disjoint(plannedLeft, plannedRight);
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


	public TrainProcessor(@NonNull List<Block> blocks){
		if (blocks.size() == 0){
			throw new IllegalArgumentException("No journeys given");
		}
		_processors = new ArrayList<BlockProcessor>(blocks.size());
		for (Block b : blocks){
			_processors.add(new BlockProcessor(b));
		}
	}

	/**
	 * @param routeId will be set to all blocks in this train journey
	 */

	public void setRouteId(@NonNull Long routeId){
		for (BlockProcessor jp : _processors){
			jp.getBlock().getSegments().get(0).setRouteId(routeId);
		}
	}

	public void changeService(@NonNull RIDservice ridService,ServiceInfoServiceType info) throws ParseException{
		HashSet<String> plannedPath = new HashSet<String>();
		for (BlockProcessor p : _processors){
			plannedPath.addAll(p.getStations());
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
				for (BlockProcessor jp : _processors){
					try{
						jp.addStoppoint(ridService,s, lastStation);
					}catch (Exception e){
						_log.error("Add station {} failed",s.getStopCode(),e);
					}
				}
			}
			lastStation = s.getStopCode().toLowerCase();
		}
	}

	public static TrainProcessor fromArnu(@NonNull RIDservice ridService,@NonNull ServiceInfoServiceType info){
		TrainProcessor p = new TrainProcessor();
		p._processors.add(BlockProcessor.fromArnu(ridService, info));
		return p;
	}

	public GtfsRealtimeIncrementalUpdate process(ServiceInfoServiceType info){
		GtfsRealtimeIncrementalUpdate update = new GtfsRealtimeIncrementalUpdate();
		for (BlockProcessor p : _processors){
			FeedEntity.Builder entity = FeedEntity.newBuilder();
			List<TripUpdate.Builder> updates = p.process(info);
			if (updates == null){
				continue;
			}
			for (TripUpdate.Builder u : updates){
				entity.setTripUpdate(u);
				entity.setId(p.getId());
				update.addUpdatedEntity(entity.build());
			}
		}
		return update;
	}
	
	/**
	 * @param originalTrainNumber trainnumber of original (superset) trip
	 * @return routeId of journey with originalTrainNumber
	 */

	public Long getRouteId(Integer originalTrainNumber) {
		for (BlockProcessor bp : _processors){
			for (Journey j : bp.getBlock().getSegments()){
				if (j.getPrivateCode() != null && j.getPrivateCode().endsWith(":"+originalTrainNumber)){
					return j.getRouteId();
				}
			}
		}
		return null;
	}
}
