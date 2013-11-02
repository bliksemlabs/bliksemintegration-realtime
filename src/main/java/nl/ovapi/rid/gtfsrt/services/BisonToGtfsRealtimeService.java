package nl.ovapi.rid.gtfsrt.services;

import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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

import lombok.Setter;
import nl.ovapi.ZeroMQUtils;
import nl.ovapi.bison.model.DataOwnerCode;
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
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.StopPoint;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

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

import com.google.transit.realtime.GtfsRealtime.Alert;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.Position.Builder;
import com.google.transit.realtime.GtfsRealtime.TimeRange;
import com.google.transit.realtime.GtfsRealtime.TranslatedString;
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiVehiclePosition;

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
					if (msg.getMessagePriority() == MessagePriority.COMMERCIAL)
						continue;
					String id = String.format("KV15:%s:%s:%s", msg.getDataOwnerCode().name(),msg.getMessageCodeDate(),msg.getMessageCodeNumber());
					if (msg.getIsDelete()){
						update.addDeletedEntity(id);
						_log.info("Deleted KV15 {} : {}",id,msg);
						continue;
					}
					_log.info("Add KV15 {} : {}",id,msg);
					FeedEntity.Builder entity = FeedEntity.newBuilder();
					Alert.Builder alert = Alert.newBuilder();
					entity.setId(id);
					if (msg.getMessageStartTime() != null || msg.getMessageEndTime() != null){
						TimeRange.Builder timeRange = TimeRange.newBuilder();
						if (msg.getMessageStartTime() != null){
							timeRange.setStart(msg.getMessageStartTime().longValue());
						}
						if (msg.getMessageEndTime() != null){
							timeRange.setEnd(msg.getMessageEndTime().longValue());
						}
						alert.addActivePeriod(timeRange);
					}
					alert.setCause(BisonToGtfsUtils.getCause(msg));
					alert.setEffect(BisonToGtfsUtils.getEffect(msg));
					TranslatedString.Builder translation = TranslatedString.newBuilder();
					Translation.Builder text = Translation.newBuilder();
					text.setLanguage("nl");
					text.setText(BisonToGtfsUtils.text(msg));
					translation.addTranslation(text);
					alert.setDescriptionText(translation);
					if (msg.getMessageContent() != null){
						translation = TranslatedString.newBuilder();
						text = Translation.newBuilder();
						text.setLanguage("nl");
						text.setText(msg.getMessageContent());
						translation.addTranslation(text);
						alert.setHeaderText(translation);
					}
					if (msg.getUserstopCodes().size() > 0){
						for (String userstopcode : msg.getUserstopCodes()){
							ArrayList<Long> stopIds = _ridService.getStopIds(msg.getDataOwnerCode(), userstopcode);
							if (stopIds != null){
								for (Long stopId : stopIds) {
									if (msg.getLinePlanningNumbers().size() == 0){
										EntitySelector.Builder selector = EntitySelector.newBuilder();
										selector.setStopId(stopId.toString());
										alert.addInformedEntity(selector);
									}else{
										for (String linePlanningNumber : msg.getLinePlanningNumbers()){
											ArrayList<String> lineIds = _ridService.getLineIds(msg.getDataOwnerCode(), linePlanningNumber);
											for (String lineId : lineIds){ //Restrict alert to linenumbers
												EntitySelector.Builder selector = EntitySelector.newBuilder();
												selector.setStopId(stopId.toString());
												selector.setRouteId(lineId);
												alert.addInformedEntity(selector);
											}
										}
									}
								}
							}
						}
					}else if (msg.getLinePlanningNumbers().size() > 0){
						for (String linePlanningNumber : msg.getLinePlanningNumbers()){
							ArrayList<String> lineIds = _ridService.getLineIds(msg.getDataOwnerCode(), linePlanningNumber);
							for (String lineId : lineIds){ //Restrict alert to linenumbers
								EntitySelector.Builder selector = EntitySelector.newBuilder();
								selector.setRouteId(lineId);
								alert.addInformedEntity(selector);
							}
						}
					}
					if (alert.getInformedEntityCount() > 0){
						entity.setAlert(alert);
						update.addUpdatedEntity(entity.build());
					}
				}catch (Exception e){
					_log.error("Processing KV15 {}",msg,e);
				}
			}
			if (update.getDeletedEntities().size() > 0 || update.getUpdatedEntities().size() > 0)
				_alertsSink.handleIncrementalUpdate(update);
		}
	}

	@PostConstruct
	public void start() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
		_executor = Executors.newCachedThreadPool();
		_scheduler = Executors.newScheduledThreadPool(5);
		_scheduler.scheduleAtFixedRate(new GarbageCollectorTask(), 60, GARBAGE_COLLECTOR_INTERVAL_SECONDS, TimeUnit.SECONDS);
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

	private FeedEntity vehiclePosition(String id,Journey journey,KV6posinfo posinfo){
		FeedEntity.Builder feedEntity = FeedEntity.newBuilder();
		feedEntity.setId(id);
		VehiclePosition.Builder vehiclePosition = VehiclePosition.newBuilder();
		int delay = posinfo.getPunctuality() == null ? 0 : posinfo.getPunctuality();
		switch (posinfo.getMessagetype()){
		case END:
			return null;
		case DELAY:
			TimeDemandGroupPoint firstTimePoint = journey.getTimedemandgroup().getPoints().get(0);
			JourneyPatternPoint firstPatternPoint = journey.getJourneypattern().getPoint(firstTimePoint.getPointorder());
			vehiclePosition.setStopId(firstPatternPoint.getPointref().toString());
			vehiclePosition.setCurrentStatus(VehicleStopStatus.IN_TRANSIT_TO);
			vehiclePosition.setCurrentStopSequence(firstTimePoint.getPointorder());
			delay = Math.max(0, delay);
			break;
		case INIT:
		case ARRIVAL:
		case ONSTOP:
			for (JourneyPatternPoint point : journey.getJourneypattern().getPoints()){
				if (point.getOperatorpointref().equals(posinfo.getUserstopcode())){
					vehiclePosition.setStopId(point.getPointref().toString());
					vehiclePosition.setCurrentStopSequence(point.getPointorder());
					vehiclePosition.setCurrentStatus(VehicleStopStatus.STOPPED_AT);
					StopPoint sp = _ridService.getStopPoint(point.getPointref());
					if ((posinfo.getRd_x() == null || posinfo.getRd_x() == -1) && sp != null){
						Builder position = Position.newBuilder();
						position.setLatitude(sp.getLatitude());
						position.setLongitude(sp.getLongitude());
						vehiclePosition.setPosition(position);
					}
					if (point.isWaitpoint() && delay < 0){
						delay = 0;
					}
				}
			}
			break;
		case DEPARTURE:
		case OFFROUTE:
		case ONROUTE:
			boolean passed = false;
			for (JourneyPatternPoint point : journey.getJourneypattern().getPoints()){
				if (point.getOperatorpointref().equals(posinfo.getUserstopcode())){
					passed = true;
					StopPoint sp = _ridService.getStopPoint(point.getPointref());
					vehiclePosition.setCurrentStopSequence(point.getPointorder());
					if (posinfo.getMessagetype() == Type.DEPARTURE && sp != null){
						Builder position = Position.newBuilder();
						position.setLatitude(sp.getLatitude());
						position.setLongitude(sp.getLongitude());
						vehiclePosition.setPosition(position);
						if (delay < 0 &&  point.isWaitpoint()){
							delay = 0;
						}
					}
				}else if (passed && point.isScheduled()){
					vehiclePosition.setStopId(point.getPointref().toString());
					vehiclePosition.setCurrentStopSequence(point.getPointorder());
					vehiclePosition.setCurrentStatus(VehicleStopStatus.IN_TRANSIT_TO);
				}
			}
			break;
		}
		if (posinfo.getRd_x() != null){
			Position position = _geometryService.toWGS84(posinfo.getRd_x(), posinfo.getRd_y());
			if (position != null)
				vehiclePosition.setPosition(position);
		}
		TripDescriptor.Builder tripDescription = journey.tripDescriptor();
		if (posinfo.getReinforcementnumber() > 0){
			tripDescription.setScheduleRelationship(ScheduleRelationship.ADDED);
		}
		vehiclePosition.setTrip(tripDescription);
		if (posinfo.getVehicleDescription() != null)
			vehiclePosition.setVehicle(posinfo.getVehicleDescription());
		vehiclePosition.setTimestamp(posinfo.getTimestamp());
		if (posinfo.getPunctuality() != null){
			OVapiVehiclePosition.Builder ovapiVehiclePosition = OVapiVehiclePosition.newBuilder();
			if (vehiclePosition.hasCurrentStopSequence() && vehiclePosition.getCurrentStopSequence() <= 1 && delay < 0){
				delay = 0;
			}
			ovapiVehiclePosition.setDelay(delay);
			vehiclePosition.setExtension(GtfsRealtimeOVapi.ovapiVehiclePosition, ovapiVehiclePosition.build());
		}
		feedEntity.setVehicle(vehiclePosition);
		return feedEntity.build();
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
					Journey j = _ridService.getJourney(id);
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
		if (posinfo.getDataownercode() == DataOwnerCode.GVB){
			String newId = _ridService.getGVBdeltaId(id);
			if (newId != null){
				return newId;
			}
			_log.info("GVB delta ID not found {}",id);
		}
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
					Journey journey = _ridService.getJourney(id);
					if (journey == null){
						Calendar c = Calendar.getInstance();
						if (posinfo.getDataownercode() == DataOwnerCode.CXX && c.get(Calendar.HOUR_OF_DAY) < 4){//Connexxion operday fuckup workaround
							SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
							c.setTime(df.parse(posinfo.getOperatingday()));
							c.add(Calendar.DAY_OF_YEAR, -1);
							posinfo.setOperatingday(df.format(c.getTime()));
							id = getId(posinfo);
							journey = _ridService.getJourney(id);
						}
						if (journey == null){ //Double check for the CXX workaround
							_log.info("Journey {} not found",id);
							continue; //Trip not in database
						}
					}
					if (posinfo.getReinforcementnumber() > 0)
						id += ":"+posinfo.getReinforcementnumber().toString(); // Key for reinforcement
					if (posinfo.getMessagetype() == Type.END){
						if (posinfo.getReinforcementnumber() == 0)
							journey.clearKV6(); //Primary vehicle finished
						else if (journey.getReinforcements().containsKey(posinfo.getReinforcementnumber()))
							journey.getReinforcements().remove(posinfo.getReinforcementnumber()); //Remove reinforcement
						vehicleUpdates.addDeletedEntity(id);
					}
					FeedEntity vehiclePosition = vehiclePosition(id,journey,posinfo);
					if (vehiclePosition != null){
						vehicleUpdates.addUpdatedEntity(vehiclePosition);
						if (posinfo.getReinforcementnumber() > 0){
							journey.getReinforcements().put(posinfo.getReinforcementnumber(), posinfo);
						}
					}
					if (posinfo.getReinforcementnumber() == 0){ //Primary vehicle, BISON can currently not yet support schedules for reinforcments
						try{
							TripUpdate.Builder tripUpdate = journey.update(posinfo);
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
					Journey journey = _ridService.getJourney(id);
					if (journey == null){
						_log.info("KV17: journey {} not found",id);
						continue; //Journey not found
					}
					TripUpdate.Builder tripUpdate = journey.update(cvlinfos);
					if (tripUpdate != null){
						FeedEntity.Builder entity = FeedEntity.newBuilder();
						entity.setTripUpdate(journey.update(cvlinfos));
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

	private class ReceiveTask implements Runnable {
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
			Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect(pubAdress);
			subscriber.subscribe("".getBytes());
			org.zeromq.ZMQ.Poller poller = context.poller();
			poller.register(subscriber);
			while (!Thread.interrupted()) {
				if (poller.poll(60*1000*5) > 0){
					messagecounter++;
					if (messagecounter % 1000 == 0){
						_log.debug(messagecounter + " BISON messages received");
					}
					try {
						String[] m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						if (m[0].toLowerCase().endsWith("kv6posinfo")) {
							InputSource s = new InputSource(new StringReader(m[1]));
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
				}else{
					subscriber.disconnect(pubAdress);
					_log.error("Connection to {} lost, reconnecting",pubAdress);
					subscriber.connect(pubAdress);
					subscriber.subscribe("".getBytes());
				}
			}
			_log.error("BisonToGtfsRealtime service interrupted");
			subscriber.disconnect(pubAdress);
		}
	}
}
