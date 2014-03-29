package nl.ovapi.rid.gtfsrt.services;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import lombok.NonNull;
import lombok.Setter;
import nl.ovapi.ZeroMQUtils;
import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.JourneyProcessor;
import nl.ovapi.bison.model.KV15message;
import nl.ovapi.bison.model.KV17cvlinfo;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.bison.model.MessagePriority;
import nl.ovapi.bison.sax.BisonToGtfsUtils;
import nl.ovapi.bison.sax.KV15SAXHandler;
import nl.ovapi.bison.sax.KV17SAXHandler;
import nl.ovapi.bison.sax.KV6SAXHandler;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.TooOldException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.model.Journey;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.Alerts;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.TripUpdates;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.VehiclePositions;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeIncrementalUpdate;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSink;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.Alert.Builder;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

@Singleton
public class BisonToGtfsRealtimeService {

	private GtfsRealtimeSink _tripUpdatesSink;
	private GtfsRealtimeSink _vehiclePositionsSink;
	private GtfsRealtimeSink _alertsSink;
	@Setter String pubAdress;
	private ExecutorService _executor;
	private Future<?> _task;
	private ScheduledExecutorService _scheduler;
	private GeometryService _geometryService;
	private RIDservice _ridService;
	private static final Logger _log = LoggerFactory.getLogger(BisonToGtfsRealtimeService.class);
	private final static int GARBAGE_COLLECTOR_INTERVAL_SECONDS = 60;
	private final static int POSINFO_MAX_AGE_SECONDS = 120;
	private final static int TRIPUPDATE_EXPIRATION_HOURS = 1;

	private GtfsRealtimeSource _tripUpdatesSource;
	private GtfsRealtimeSource _vehiclePositionsSource;
	private Map<String, JourneyProcessor> journeyProcessors;

	@Inject
	public void setTripUpdatesSource(@TripUpdates GtfsRealtimeSource tripUpdatesSource) {
		_tripUpdatesSource = tripUpdatesSource;
	}

	@Inject
	public void setVehiclePositionsSource(@VehiclePositions	GtfsRealtimeSource vehiclePositionsSource) {
		_vehiclePositionsSource = vehiclePositionsSource;
	}

	@Inject
	public void setTripUpdatesSink(@TripUpdates	GtfsRealtimeSink tripUpdatesSink) {
		_tripUpdatesSink = tripUpdatesSink;
	}

	@Inject
	public void setAlertsSink(@Alerts GtfsRealtimeSink alertsSink) {
		_alertsSink = alertsSink;
	}

	@Inject
	public void setVehiclePositionsSink(@VehiclePositions GtfsRealtimeSink vehiclePositionsSink) {
		_vehiclePositionsSink = vehiclePositionsSink;
	}

	@Inject
	public void setGeometryService(GeometryService geometryService) {
		_geometryService = geometryService;
	}

	@Inject
	public void setRIDService(RIDservice ridService) {
		_ridService = ridService;
	}

	private class ProcessKV15Task implements Runnable{
		private ArrayList<KV15message> messages;
		public ProcessKV15Task(ArrayList<KV15message> messages){
			this.messages = messages;
		}
		@Override
		public void run() {
			GtfsRealtimeIncrementalUpdate update = new GtfsRealtimeIncrementalUpdate();
			for (KV15message msg : messages){
				try{
					String id = String.format("KV15:%s:%s:%s", msg.getDataOwnerCode().name(),msg.getMessageCodeDate(),msg.getMessageCodeNumber());
					if (msg.getIsDelete()){
						update.addDeletedEntity(id);
						_log.info("Deleted KV15 {} : {}",id,msg);
						continue;
					}
					if (msg.getMessagePriority() == MessagePriority.COMMERCIAL && msg.getDataOwnerCode() != DataOwnerCode.QBUZZ){
						_log.info("Ignore KV15 {}",msg);
						continue;
					}
					FeedEntity.Builder entity = FeedEntity.newBuilder();
					entity.setId(id);
					Builder alert = BisonToGtfsUtils.translateKV15ToGTFSRT(msg, _ridService);
					if (alert.getInformedEntityCount() > 0){
						_log.info("Add KV15 {} : {}",id,msg);
						entity.setAlert(alert);
						update.addUpdatedEntity(entity.build());
					}else{
						_log.info("Ignore KV15, not entities found{}",msg);
					}
				}catch (Exception e){
					_log.error("Processing KV15 {}",msg,e);
				}
			}
			if (update.getDeletedEntities().size() > 0 || update.getUpdatedEntities().size() > 0)
				_alertsSink.handleIncrementalUpdate(update);
		}
	}

	private JourneyProcessor getOrCreateProcessorForId(@NonNull String privateCode){
		JourneyProcessor jp = journeyProcessors.get(privateCode);
		if (jp != null){
			return jp;
		}
		Journey journey = _ridService.getJourney(privateCode);
		if (journey == null){
			_log.info("Journey {} not found",privateCode);
			return null; //Journey not found
		}
		jp = new JourneyProcessor(journey);
		journeyProcessors.put(privateCode, jp);
		return jp;
	}

	@PostConstruct
	public void start() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
		journeyProcessors = Maps.newHashMap();
		_executor = Executors.newCachedThreadPool();
		_scheduler = Executors.newScheduledThreadPool(5);
		_scheduler.scheduleAtFixedRate(new GarbageCollectorTask(), 60, GARBAGE_COLLECTOR_INTERVAL_SECONDS, TimeUnit.SECONDS);
		_task = _executor.submit(new ProcessTask());
		_task = _executor.submit(new ReceiveTask());
		try {
			_executor.submit(new ProcessKV15Task(_ridService.getActiveKV15messages()));
		} catch (SQLException e) {
			e.printStackTrace();
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
		if (_scheduler != null) {
			_scheduler.shutdownNow();
			_scheduler = null;
		}
	}


	public void remove(ArrayList<String> removeIds){
		if (removeIds.size() == 0){
			return;
		}
		GtfsRealtimeIncrementalUpdate vehicleUpdates = new GtfsRealtimeIncrementalUpdate();
		GtfsRealtimeIncrementalUpdate tripUpdates = new GtfsRealtimeIncrementalUpdate();
		for (String id :removeIds){
			vehicleUpdates.addDeletedEntity(id);
			tripUpdates.addDeletedEntity(id);
		}
		_vehiclePositionsSink.handleIncrementalUpdate(vehicleUpdates);
		_tripUpdatesSink.handleIncrementalUpdate(tripUpdates);
	}

	private class GarbageCollectorTask implements Runnable{
		@Override
		public void run() {
			//Delete vehicle updates that haven't received KV6 in 2 minutes.
			GtfsRealtimeIncrementalUpdate vehicleUpdates = new GtfsRealtimeIncrementalUpdate();
			GtfsRealtimeIncrementalUpdate tripUpdates = new GtfsRealtimeIncrementalUpdate();
			long threshold = Utils.currentTimeSecs() - POSINFO_MAX_AGE_SECONDS;
			int vehiclesCleaned = 0;
			int tripsCleaned = 0;
			for (FeedEntity f : _vehiclePositionsSource.getFeed().getEntityList()){		
				try{
					String[] idParts = f.getId().split(":");
					String id = f.getId();
					if (idParts.length == 5){
						id = String.format("%s:%s:%s:%s", idParts[0],idParts[1],idParts[2],idParts[3]);
					}
					JourneyProcessor j = getOrCreateProcessorForId(id);
					if (!f.hasVehicle() || f.getVehicle().getTimestamp() < threshold){
						if (j != null && idParts.length == 4){
							j.clearKV6();
						}else if (j != null && idParts.length == 5){
							Integer reinforcementNumber = Integer.valueOf(idParts[4]);
							j.getReinforcements().remove(reinforcementNumber);
						}
						vehicleUpdates.addDeletedEntity(f.getId());
						_log.trace("Garbage cleaned {}",f.getId());
						vehiclesCleaned++;
					}
				}catch (Exception e){
					_log.error("Garbage Collection tripUpdates",e);
				}
			}
			for (FeedEntity f : _tripUpdatesSource.getFeed().getEntityList()){
				try{
					if (!f.hasTripUpdate() ||( f.getTripUpdate().hasTimestamp() && f.getTripUpdate().getTimestamp() < threshold)){
						Journey j = _ridService.getJourney(f.getId());
						if (j == null){
							tripUpdates.addDeletedEntity(f.getId());
							_log.trace("Garbage cleaned -> Journey Null {}",f.getId());
							tripsCleaned++;
						}else if (j.getEndEpoch() < (Utils.currentTimeSecs()-TRIPUPDATE_EXPIRATION_HOURS*60*60)){ //
							tripUpdates.addDeletedEntity(f.getId());
							_log.trace("Garbage cleaned {}",f.getId());
							tripsCleaned++;
						}
					}
				}catch (Exception e){
					_log.error("Garbage Collection tripUpdates",e);
				}
			}
			_log.info("GarbageCollector: {} vehicles cleaned, {} trips cleaned",vehiclesCleaned,tripsCleaned);
			if (vehicleUpdates.getDeletedEntities().size() > 0 || vehicleUpdates.getUpdatedEntities().size() > 0)
				_vehiclePositionsSink.handleIncrementalUpdate(vehicleUpdates);
			if (tripUpdates.getDeletedEntities().size() > 0 || tripUpdates.getUpdatedEntities().size() > 0)
				_tripUpdatesSink.handleIncrementalUpdate(tripUpdates);
		}
	}

	private String getId(KV6posinfo posinfo){
		String id = String.format("%s:%s:%s:%s", 
				posinfo.getOperatingday(),
				posinfo.getDataownercode().name(),
				posinfo.getLineplanningnumber(),
				posinfo.getJourneynumber());
		return id;
	}
	private class ProcessKV6Task implements Runnable{
		private ArrayList<KV6posinfo> posinfos;
		public ProcessKV6Task(ArrayList<KV6posinfo> posinfos){
			this.posinfos = posinfos;
		}
		@Override
		public void run() {
			GtfsRealtimeIncrementalUpdate tripUpdates = new GtfsRealtimeIncrementalUpdate();
			GtfsRealtimeIncrementalUpdate vehicleUpdates = new GtfsRealtimeIncrementalUpdate();
			for (KV6posinfo posinfo : posinfos){
				try{
					if (posinfo.getLineplanningnumber() == null || "".equals(posinfo.getLineplanningnumber())){
						continue;
					}
					String id = getId(posinfo);
					JourneyProcessor jp = getOrCreateProcessorForId(id);
					if (jp == null){
						Calendar c = Calendar.getInstance();
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
						c.setTime(df.parse(posinfo.getOperatingday()));
						if (_ridService.getFromDate() > c.getTimeInMillis()){
							continue;
						}
						if (posinfo.getDataownercode() == DataOwnerCode.CXX && c.get(Calendar.HOUR_OF_DAY) < 4){//Connexxion operday fuckup workaround
							c.add(Calendar.DAY_OF_YEAR, -1);
							posinfo.setOperatingday(df.format(c.getTime()));
							id = getId(posinfo);
							jp = getOrCreateProcessorForId(id);
						}
						if (posinfo.getDataownercode() == DataOwnerCode.GVB){
							String newId = _ridService.getGVBdeltaId(id);
							if (newId != null){
								id = newId;
							}else{
								_log.info("GVB delta ID not found {}",id);
							}
							jp = getOrCreateProcessorForId(id);
						}
						if (jp == null){ //Double check for the CXX workaround
							_log.info("Journey {} not found",id);
							continue; //Trip not in database
						}
					}
					if (posinfo.getReinforcementnumber() > 0)
						id += ":"+posinfo.getReinforcementnumber().toString(); // Key for reinforcement
					if (posinfo.getMessagetype() == Type.END){
						if (posinfo.getReinforcementnumber() == 0)
							jp.clearKV6(); //Primary vehicle finished
						else if (jp.getReinforcements().containsKey(posinfo.getReinforcementnumber()))
							jp.getReinforcements().remove(posinfo.getReinforcementnumber()); //Remove reinforcement
						vehicleUpdates.addDeletedEntity(id);
					}
					FeedEntity vehiclePosition = jp.vehiclePosition(id,jp,posinfo,_ridService,_geometryService);
					if (vehiclePosition != null){
						vehicleUpdates.addUpdatedEntity(vehiclePosition);
						if (posinfo.getReinforcementnumber() > 0){
							jp.getReinforcements().put(posinfo.getReinforcementnumber(), posinfo);
						}
					}
					if (posinfo.getReinforcementnumber() == 0){ //Primary vehicle, BISON can currently not yet support schedules for reinforcments
						try{
							TripUpdate.Builder tripUpdate = jp.update(posinfo);
							if (tripUpdate != null){
								FeedEntity.Builder tripEntity = FeedEntity.newBuilder();
								tripEntity.setId(id);
								tripEntity.setTripUpdate(tripUpdate); //Get update created from KV6
								tripUpdates.addUpdatedEntity(tripEntity.build());
								boolean valid = true;
								int pointorder = -1;
								for (StopTimeUpdate stoptimeUpdate : tripUpdate.getStopTimeUpdateList()){
									if (stoptimeUpdate.getStopSequence() < pointorder){
										valid = false;
									}
									pointorder = stoptimeUpdate.getStopSequence();
								}
								if (!valid){
									_log.error("Invalid sequence of pointorders {}",tripUpdate.build());
								}
							}
						}catch (TooOldException e){
							_log.info("Trip {} Too old: {}", id,posinfo);
						}catch (StopNotFoundException e){
							_log.info("Trip {} userstop {} not found", id,posinfo.getUserstopcode());
						}catch (TooEarlyException e){
							_log.trace("Trip {} punctuality too early {}", id,posinfo);
						} catch (UnknownKV6PosinfoType e) {
							_log.info("Trip {} unknown Posinfotype {}", id,posinfo);
						}
					}
				}catch (Exception e){
					e.printStackTrace(System.err);
					_log.error("Exception {}",posinfo,e);
				}
			}
			if (vehicleUpdates.getDeletedEntities().size() > 0 || vehicleUpdates.getUpdatedEntities().size() > 0)
				_vehiclePositionsSink.handleIncrementalUpdate(vehicleUpdates);
			if (tripUpdates.getDeletedEntities().size() > 0 || tripUpdates.getUpdatedEntities().size() > 0)
				_tripUpdatesSink.handleIncrementalUpdate(tripUpdates);
		}
	}

	private class ProcessKV17Task implements Runnable{
		private ArrayList<KV17cvlinfo> cvlinfos;
		public ProcessKV17Task(ArrayList<KV17cvlinfo> cvlinfos){
			this.cvlinfos = cvlinfos;
		}
		@Override
		public void run() {
			HashMap<String,ArrayList<KV17cvlinfo>> map = new HashMap<String,ArrayList<KV17cvlinfo>>();
			GtfsRealtimeIncrementalUpdate tripUpdates = new GtfsRealtimeIncrementalUpdate();
			try{
				for (KV17cvlinfo cvlinfo : cvlinfos){
					String id = String.format("%s:%s:%s:%s", cvlinfo.getOperatingday(),cvlinfo.getDataownercode().name(),cvlinfo.getLineplanningnumber(),cvlinfo.getJourneynumber());
					if (!map.containsKey(id)){
						map.put(id, new ArrayList<KV17cvlinfo>());
					}
					map.get(id).add(cvlinfo);
				}
				for (String id : map.keySet()){
					ArrayList<KV17cvlinfo> cvlinfos = map.get(id);
					JourneyProcessor jp = getOrCreateProcessorForId(id);
					TripUpdate.Builder tripUpdate = jp.update(cvlinfos);
					if (tripUpdate != null){
						FeedEntity.Builder entity = FeedEntity.newBuilder();
						entity.setTripUpdate(tripUpdate);
						entity.setId(id);
						tripUpdates.addUpdatedEntity(entity.build());
					}
				}
			}catch (Exception e){
				_log.error("ProcessKV17Task",e);
			}
			if (tripUpdates.getDeletedEntities().size() > 0 || tripUpdates.getUpdatedEntities().size() > 0)
				_tripUpdatesSink.handleIncrementalUpdate(tripUpdates);
		}
	}

	void process(ArrayList<KV6posinfo> posinfos){
		_executor.submit(new ProcessKV6Task(posinfos));
	}

	public boolean startsWithBom(String line) {  
		char myChar = line.charAt(0);  
		int intValue = (int) myChar;  
		// Hexa value of BOM = EF BB BF  => int 65279  
		if (intValue == 65279) {  
			return true;  
		} else {  
			return false;  
		}  
	}  

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
			final String PULL_ADDRESS = "tcp://127.0.0.1:"+INPROC_PORT;
			pull.connect(PULL_ADDRESS);
			while (!Thread.interrupted()) {
				messagecounter++;
				if (messagecounter % 1000 == 0){
					_log.debug(messagecounter + " BISON messages received");
				}
				try {
					String[] m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(pull));
					if (startsWithBom(m[1])){
						m[1] = m[1].substring(1);
					}
					if (m[0].toLowerCase().endsWith("kv6posinfo")) {
						InputSource s = new InputSource(new StringReader(m[1]));
						s.setEncoding("UTF-8");
						KV6SAXHandler handler = new KV6SAXHandler();
						xr.setContentHandler(handler);
						try {
							xr.parse(s);
							process(handler.getPosinfos());
						} catch (Exception e) {
							_log.error("KV6 parsing {}",m[1],e);
						}
					} else if (m[0].toLowerCase().endsWith("kv17cvlinfo")) {
						InputSource s = new InputSource(new StringReader(m[1]));
						s.setEncoding("UTF-8");
						KV17SAXHandler handler = new KV17SAXHandler();
						xr.setContentHandler(handler);
						try {
							xr.parse(s);
							_executor.submit(new ProcessKV17Task(handler.getCvlinfos()));
							System.out.println(m[1]);
						} catch (Exception e) {
							_log.error("KV17 parsing {}",m[1],e);
						}
					} else if (m[0].toLowerCase().endsWith("kv15messages")) {
						InputSource s = new InputSource(new StringReader(m[1]));
						s.setEncoding("UTF-8");
						KV15SAXHandler handler = new KV15SAXHandler();
						xr.setContentHandler(handler);
						try {
							xr.parse(s);
							_executor.submit(new ProcessKV15Task(handler.getMessages()));
						} catch (Exception e) {
							_log.error("KV15 parsing {}",m[1]);
						}
					} else {
						_log.error("Unknown URL {}",m[0]);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}	
			}
			_log.error("BisonToGtfsRealtime service interrupted");
			pull.disconnect(PULL_ADDRESS);
		}
	}

	private final static String INPROC_PORT = "51545";

	private class ReceiveTask implements Runnable {
		@Override
		public void run() {
			Context context = ZMQ.context(1);
			Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect(pubAdress);
			subscriber.subscribe("".getBytes());
			Socket push = context.socket(ZMQ.PUSH);
			push.setSndHWM(500000);
			push.bind("tcp://*:"+INPROC_PORT);
			_log.info("Connect to {}",pubAdress);
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
					subscriber.disconnect(pubAdress);
					subscriber.connect(pubAdress);
					_log.error("Connection to {} lost, reconnecting",pubAdress);
					subscriber.subscribe("".getBytes());
				}
			}
			subscriber.disconnect(pubAdress);
		}
	}
}
