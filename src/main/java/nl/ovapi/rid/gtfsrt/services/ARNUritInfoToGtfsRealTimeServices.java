package nl.ovapi.rid.gtfsrt.services;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import nl.ovapi.arnu.TrainProcessor;
import nl.ovapi.rid.model.Journey;
import nl.tt_solutions.schemas.ns.rti._1.PutServiceInfoIn;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoKind;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.TripUpdates;
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
	private Future<?> _task;
	private static final Logger _log = LoggerFactory.getLogger(ARNUritInfoToGtfsRealTimeServices.class);
	private final static String pubAddress = "tcp://post.ndovloket.nl:7662";
	private GtfsRealtimeSink _tripUpdatesSink;
	private RIDservice _ridService;
	private Map<String, TrainProcessor> journeyProcessors;


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
		journeyProcessors = Maps.newHashMap();
		_task = _executor.submit(new ProcessTask());
		_task = _executor.submit(new ReceiveTask());
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
		TrainProcessor jp = journeyProcessors.get(id);
		if (jp != null){
			return jp;
		}
		List<Journey> trains = _ridService.getTrains(id);
		if (trains == null || trains.size() == 0){
			return null; //Journey not found
		}
		jp = new TrainProcessor(trains);
		journeyProcessors.put(id, jp);
		return jp;
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
						switch(info.getServiceType()){
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
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
						String id = String.format("%s:IFF:%s:%s",df.format(new Date()),info.getTransportModeCode(),info.getServiceCode());
						TrainProcessor jp = getOrCreateProcessorForId(id);
						if (jp == null && info.getServiceType() != ServiceInfoKind.NORMAL_SERVICE){
							jp = TrainProcessor.fromArnu(_ridService,info);
							journeyProcessors.put(id, jp);
						}
						if (jp != null){
							if (info.getServiceType() != null){
								switch (info.getServiceType()){
								case NORMAL_SERVICE:
								case NEW_SERVICE:
								case SPLIT_SERVICE:
								case CANCELLED_SERVICE:
									break;
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
					_log.error("Error ARNU",e);
					e.printStackTrace();
				}	
			}
			_log.error("ARNU2GTFSrealtime service interrupted");
			pull.disconnect(PULL_ADDRESS);
		}
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
