package nl.ovapi.arnu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import lombok.NonNull;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern;
import nl.ovapi.rid.model.TimeDemandGroup;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
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
	
	public TrainProcessor(@NonNull List<Journey> journeys){
		_processors = new ArrayList<JourneyProcessor>(journeys.size());
		for (Journey j : journeys){
			_processors.add(new JourneyProcessor(j));
		}
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
			pt.setPointorder(i);
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
	
	public static int secondsSinceMidnight(Calendar c){
		return c.get(Calendar.HOUR_OF_DAY)*60*60+c.get(Calendar.MINUTE)*60+c.get(Calendar.SECOND);
	}

	
	public static TrainProcessor fromArnu(@NonNull RIDservice ridService,@NonNull ServiceInfoServiceType info){
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
		List<Journey> journeys = new ArrayList<Journey>();
		journeys.add(j);
		TrainProcessor p = new TrainProcessor(journeys);
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
