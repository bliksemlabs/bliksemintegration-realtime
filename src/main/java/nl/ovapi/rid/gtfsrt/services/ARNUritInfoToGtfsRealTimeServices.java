package nl.ovapi.rid.gtfsrt.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;

import nl.ovapi.ZeroMQUtils;
import nl.tt_solutions.schemas.ns.rti._1.PutServiceInfoIn;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.Alerts;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.XMLReader;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

@Singleton
public class ARNUritInfoToGtfsRealTimeServices {

	private ExecutorService _executor;
	private Future<?> _task;
	private static final Logger _log = LoggerFactory.getLogger(ARNUritInfoToGtfsRealTimeServices.class);
	private final static String pubAddress = "tcp://post.ndovloket.nl:7662";
	private GtfsRealtimeSink _alertsSink;
	private RIDservice _ridService;
	private Map<String, ArrayList<String>> stations;


	@Inject
	public void setRIDService(RIDservice ridService) {
		_ridService = ridService;
	}

	@Inject
	public void setTripUpdatesSink(@Alerts GtfsRealtimeSink alertsSink) {
		_alertsSink = alertsSink;
	}

	@PostConstruct
	public void start() {
		_executor = Executors.newCachedThreadPool();
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

	private final static String INPROC_PORT = "51546";

	private class ProcessTask implements Runnable {
		int messagecounter = 0;
		@Override
		public void run() {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			SAXParser sp;
			XMLReader xr = null;
			try {sp = spf.newSAXParser();
			xr = sp.getXMLReader();} catch (Exception e) {return;}
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
					System.out.println(m[0]);
					InputStream stream = new ByteArrayInputStream(m[1].getBytes("UTF-8"));
					JAXBElement<PutServiceInfoIn> feed = unmarshaller.unmarshal(new StreamSource(stream), PutServiceInfoIn.class);
					if (feed == null || feed.getValue() == null || feed.getValue().getServiceInfoList() == null){
						continue;
					}
					for (ServiceInfoServiceType info : feed.getValue().getServiceInfoList().getServiceInfo()){
						String id = String.format("IFF:%s:%s:%s", info.getCompanyCode().toUpperCase(),info.getTransportModeCode(),info.getServiceCode());
						System.out.println(id);
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
