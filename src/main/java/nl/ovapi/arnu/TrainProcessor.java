package nl.ovapi.arnu;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.model.Journey;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopType;

/**
 * Keeps collection of journeys with the same trainnumber in different blocks
 * @author Thomas Koch
 */
public class TrainProcessor {

	private List<JourneyProcessor> _processors;
	
	public TrainProcessor(@NonNull List<Journey> journeys){
		_processors = new ArrayList<JourneyProcessor>(journeys.size());
		for (Journey j : journeys){
			_processors.add(new JourneyProcessor(j));
		}
	}
		
	public void process(ServiceInfoServiceType info){
		for (JourneyProcessor p : _processors){
			p.process(info);
		}
	}
}
