package nl.ovapi.rid.gtfsrt.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import lombok.NonNull;
import nl.ovapi.ZeroMQUtils;
import nl.ovapi.arnu.ARNUexporter;
import nl.ovapi.arnu.TrainProcessor;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.model.Block;
import nl.tt_solutions.schemas.ns.rti._1.PutServiceInfoIn;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoKind;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoStopType;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.TripUpdates;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeIncrementalUpdate;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import com.google.common.collect.Maps;

@Singleton
public class ARNUritInfoToGtfsRealTimeServices {

	private ExecutorService _executor;
	private ScheduledExecutorService _scheduler;
	private Future<?> _task;
	private static final Logger _log = LoggerFactory.getLogger(ARNUritInfoToGtfsRealTimeServices.class);
	private final static String pubAddress = "tcp://post.ndovloket.nl:7662";
	private final static int GARBAGE_COLLECTOR_INTERVAL_SECONDS = 60;
	private final static int TRIPUPDATE_EXPIRATION_HOURS = 1;
	private GtfsRealtimeSink _tripUpdatesSink;
	private RIDservice _ridService;
	private ConcurrentMap<String, TrainProcessor> journeyProcessors;
	private ARNUexporter _arnuExporter;
	
	@Inject
	public void setARnuExporter(ARNUexporter arnuExporter) {
		_arnuExporter = arnuExporter;
	}
	
	@Inject
	public void setRIDService(RIDservice ridService) {
		_ridService = ridService;
	}

	@Inject
	public void setTripUpdatesSink(@TripUpdates GtfsRealtimeSink tripUpdates) {
		_tripUpdatesSink = tripUpdates;
	}

	@PostConstruct
	public void start() {
		_executor = Executors.newCachedThreadPool();
		_scheduler = Executors.newScheduledThreadPool(5);
		journeyProcessors = Maps.newConcurrentMap();
		_task = _executor.submit(new ProcessTask());
		_task = _executor.submit(new ReceiveTask());
		_scheduler.scheduleAtFixedRate(new GarbageCollectorTask(), GARBAGE_COLLECTOR_INTERVAL_SECONDS, GARBAGE_COLLECTOR_INTERVAL_SECONDS, TimeUnit.SECONDS);

	}



	private class GarbageCollectorTask implements Runnable{
		@Override
		public void run() {
			//Delete vehicle updates that haven't received KV6 in 2 minutes.
			GtfsRealtimeIncrementalUpdate vehicleUpdates = new GtfsRealtimeIncrementalUpdate();
			GtfsRealtimeIncrementalUpdate tripUpdates = new GtfsRealtimeIncrementalUpdate();
			int tripsCleaned = 0;

			for (Entry<String, TrainProcessor> entry : journeyProcessors.entrySet()){
				TrainProcessor jp = entry.getValue();
				try{
					if (jp.getEndEpoch() < (Utils.currentTimeSecs()-TRIPUPDATE_EXPIRATION_HOURS*60*60)){ //
						tripUpdates.addDeletedEntity(entry.getKey());
						journeyProcessors.remove(entry.getKey());
						tripsCleaned++;
						_log.trace("Garbage cleaned {}",entry.getKey());
					}
				}catch (Exception e){
					e.printStackTrace();
					_log.error("Garbage Collection tripUpdates",e);
				}
			}
			_log.error("GarbageCollector: {} trips cleaned",tripsCleaned);
			if (tripUpdates.getDeletedEntities().size() > 0 || tripUpdates.getUpdatedEntities().size() > 0)
				_tripUpdatesSink.handleIncrementalUpdate(tripUpdates);
		}
	}


	@PreDestroy
	public void stop() {
		if (_task != null) {
			_task.cancel(true);
			_task = null;
		}
		if (_executor != null) {
			_executor.shutdownNow();
			_executor = null;
		}
	}

	private TrainProcessor getOrCreateProcessorForId(@NonNull String id){
		TrainProcessor tp = journeyProcessors.get(id);
		if (tp != null){
			return tp;
		}
		List<Block> trains = _ridService.getTrains(id);
		if (trains == null || trains.size() == 0){
			return null; //Journey not found
		}
		tp = new TrainProcessor(trains);
		journeyProcessors.put(id, tp);
		return tp;
	}

	private final static String INPROC_PORT = "51546";

	private class ProcessTask implements Runnable {
		int messagecounter = 0;
		@Override
		public void run() {
			Context context = ZMQ.context(1);
			Socket pull = context.socket(ZMQ.PULL);
			pull.setRcvHWM(500000);
			JAXBContext jc = null;
			Unmarshaller unmarshaller = null;
			try {
				jc = JAXBContext.newInstance(PutServiceInfoIn.class);
				unmarshaller = jc.createUnmarshaller();
			} catch (JAXBException e1) {
				_log.error("Error with JAXB",e1);
				e1.printStackTrace();
			}
			final String PULL_ADDRESS = "tcp://127.0.0.1:"+INPROC_PORT;
			pull.connect(PULL_ADDRESS);
			while (!Thread.interrupted()) {
				messagecounter++;
				if (messagecounter % 1000 == 0){
					_log.debug(messagecounter + " BISON messages received");
				}
				try {
					String[] m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(pull));
					InputStream stream = new ByteArrayInputStream(m[1].getBytes("UTF-8"));
					JAXBElement<PutServiceInfoIn> feed = unmarshaller.unmarshal(new StreamSource(stream), PutServiceInfoIn.class);
					if (feed == null || feed.getValue() == null || feed.getValue().getServiceInfoList() == null){
						continue;
					}
					System.out.println(m[0]);
					for (ServiceInfoServiceType info : feed.getValue().getServiceInfoList().getServiceInfo()){
						switch(info.getServiceType()){ //TODO REMOVE THIS DEBUG SWITCH
						case CANCELLED_SERVICE:
						case NORMAL_SERVICE:
						case NEW_SERVICE:
						case DIVERTED_SERVICE:
						case SCHEDULE_CHANGED_SERVICE:
							break;
						case EXTENDED_SERVICE:
						case SPLIT_SERVICE:
							System.out.println(m[1]);
						default:
							break;
						}
						String id = String.format("%s:IFF:%s:%s",getDate(info),info.getTransportModeCode(),info.getServiceCode());
						TrainProcessor jp = getOrCreateProcessorForId(id);
						if (jp == null && info.getServiceType() != ServiceInfoKind.NORMAL_SERVICE){
							jp = createFromARNU(info); //No static counterpart and ServiceInfoKind not normal
							if (jp != null)            //Create from ARNU XML
								journeyProcessors.put(id, jp);
						}
						if (jp != null){
							if (info.getServiceType() != null){
								switch (info.getServiceType()){
								case NORMAL_SERVICE:
								case SPLIT_SERVICE:
								case CANCELLED_SERVICE:
									break;
								case NEW_SERVICE: //Check and if necessary modify the scheduled journey to include the changes from ARNU
								case DIVERTED_SERVICE:
								case EXTENDED_SERVICE:
								case SCHEDULE_CHANGED_SERVICE:
									jp.changeService(_ridService,info);
								default:
									break;
								}
							}
							_tripUpdatesSink.handleIncrementalUpdate(jp.process(info));
						}else{
							System.out.println(m[1]);
							_log.error("Train {} not found",id);
						}
					}
				} catch (Exception e) {
					_log.error("Error ARNU {}",e);
					e.printStackTrace();
				}	
			}
			_log.error("ARNU2GTFSrealtime service interrupted");
			pull.disconnect(PULL_ADDRESS);
		}
	}

	private String getDate(ServiceInfoServiceType info){
		if (info.getStopList() == null || info.getStopList().getStop() == null || info.getStopList().getStop().size() == 0){
			return null;
		}

		Calendar operatingDate = null;
		for (ServiceInfoStopType s : info.getStopList().getStop()){
			if (s.getDeparture() != null){
				operatingDate = s.getDeparture().toGregorianCalendar();
				break;
			}
		}
		if (operatingDate.get(Calendar.HOUR_OF_DAY) < 4){
			operatingDate.add(Calendar.DAY_OF_MONTH, -1);
		}
		operatingDate.set(Calendar.MINUTE, 0);
		//Set at 4 because we DST of operatingday not at midnight
		operatingDate.set(Calendar.HOUR_OF_DAY, 4); 
		operatingDate.set(Calendar.SECOND, 0);
		operatingDate.set(Calendar.MILLISECOND, 0);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		return df.format(operatingDate.getTime());
	}


	private TrainProcessor createFromARNU(ServiceInfoServiceType info){
		TrainProcessor jp = TrainProcessor.fromArnu(_ridService,info);
		// If possible place this new train in an existing GTFS route. 
		Integer originalTrainNumber = TrainProcessor.orginalTrainNumber(info.getServiceCode());
		if (jp != null && originalTrainNumber != null){
			TrainProcessor origJp = null;
			// Fuzzy matching for ARNU bug where split Intercity's are suddenly Sneltrein etc. 
			for (String transportModeCode : new String[] {info.getTransportModeCode(),"S","ST","SPR","HSN","IC","INT","ICE","THA","TGV"}){
				String origId = String.format("%s:IFF:%s:%s",getDate(info),transportModeCode,originalTrainNumber);	
				origJp = getOrCreateProcessorForId(origId);
				//The original journey has to be the journey this new service is a subset of.
				if (origJp != null && !jp.isDisjoint(origJp)){
					_log.debug("set routeid {} for {} ",origJp.getRouteId(originalTrainNumber),info.getServiceCode());
					//Set routeId for easier consumption of split trips 
					jp.setRouteId(origJp.getRouteId(originalTrainNumber));
					break;
				}
			}
		}
		return jp;
	}

	private class ReceiveTask implements Runnable {
		@Override
		public void run() {
			Context context = ZMQ.context(1);
			Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect(pubAddress);
			subscriber.subscribe("".getBytes());
			Socket push = context.socket(ZMQ.PUSH);
			push.setSndHWM(500000);
			push.bind("tcp://*:"+INPROC_PORT);
			_log.info("Connect to {}",pubAddress);
			@SuppressWarnings("deprecation")
			org.zeromq.ZMQ.Poller poller = context.poller();
			poller.register(subscriber);
			while (!Thread.interrupted()) {
				if (poller.poll(TimeUnit.MINUTES.toMillis(5L)) > 0){
					try{
						ZMsg.recvMsg(subscriber).send(push);
					} catch (Exception e) {
						_log.error("Error in bison receiving",e);
						e.printStackTrace();
					}
				}else{
					subscriber.disconnect(pubAddress);
					subscriber.connect(pubAddress);
					_log.error("Connection to {} lost, reconnecting",pubAddress);
					subscriber.subscribe("".getBytes());
				}
			}
			subscriber.disconnect(pubAddress);
		}
	}
}
