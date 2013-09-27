package nl.ovapi.rid.gtfsrt.services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import nl.ovapi.ZeroMQUtils;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.trein.model.AVT;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.Alerts;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeIncrementalUpdate;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.Alert;
import com.google.transit.realtime.GtfsRealtime.EntitySelector;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TranslatedString;
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation;

@Singleton
public class NSapiToGtfsRealTimeServices {

	private ExecutorService _executor;
	private Future<?> _task;
	private static final Logger _log = LoggerFactory.getLogger(NSapiToGtfsRealTimeServices.class);
	private final static String[] nsApiPublishers = new String[] {"tcp://node01.post.openov.nl:6611"};
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
		_task = _executor.submit(new ReceiveTask());
		stations = Maps.newHashMapWithExpectedSize(255);
	}

	private class CleanTask implements Runnable{
		@Override
		public void run() {
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

	private EntitySelector.Builder entitySelector(Journey j, String station, AVT avt){
		EntitySelector.Builder entitySelector = EntitySelector.newBuilder();
		String stationId = null;
		if (j == null){
			return null;//Journey does not exist
		}
		for (int i = 0; i < j.getJourneypattern().getPoints().size();i++){
			JourneyPatternPoint pt = j.getJourneypattern().getPoints().get(i);
			if (pt.getOperatorpointref().split(":")[0].equals(station)){
				stationId = pt.getPointref().toString();
				if (i < j.getJourneypattern().getPoints().size()-1){
					pt = j.getJourneypattern().getPoints().get(i);
					if (pt.getOperatorpointref().split(":")[0].equals(station)){
						stationId = pt.getPointref().toString();
					}
				}
			}
		}
		if (stationId == null){
			return null;
		}
		entitySelector.setStopId(stationId);
		entitySelector.setTrip(j.tripDescriptor());
		return entitySelector;
	}

	private void makeAlerts(String id,ArrayList<AVT> avts){
		GtfsRealtimeIncrementalUpdate update = new GtfsRealtimeIncrementalUpdate();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		ArrayList<String> alertIds = new ArrayList<String>();
		for (AVT a : avts){
			if (!a.isPlatformChanged() || (a.getRemarks() != null && a.getRemarks().length() > 0)){
				continue;
			}
			String station = id.split("/")[2].toLowerCase();
			FeedEntity.Builder entity = FeedEntity.newBuilder();
			entity.setId(id+"/"+a.getJourneynumber());
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(a.getDeparturetime()*1000);
			if (c.get(Calendar.HOUR_OF_DAY) < 4){
				c.add(Calendar.DAY_OF_MONTH, -1);
			}
			String jid = String.format("%s:IFF:%s",sdf.format(c.getTime()),a.getJourneynumber());
			ArrayList<Journey> journeys = _ridService.getTrains(jid);
			Alert.Builder alert = Alert.newBuilder();
			if (journeys == null || journeys.size() == 0){
				_log.info("Train {} not found",jid);
				continue;
			}else{
				for (Journey j : journeys){
					EntitySelector.Builder selector = entitySelector(j,station,a);
					if (selector != null)
						alert.addInformedEntity(selector);
				}
			}
			TranslatedString.Builder string = TranslatedString.newBuilder();
			Translation.Builder translation = Translation.newBuilder();
			if (a.isPlatformChanged()){
				translation.setLanguage("nl");
				translation.setText(String.format("Vertrekspoor gewijzigd naar %s", a.getDeparturePlatform()));
				string.addTranslation(translation);
				translation = Translation.newBuilder();
				translation.setText(String.format("Departure platform changed to %s", a.getDeparturePlatform()));
				string.addTranslation(translation);
				alert.setHeaderText(string);
			}else if (a.getRemarks() != null && a.getRemarks().length() > 0){
				translation.setLanguage("nl");
				translation.setText(a.getRemarks());
				string.addTranslation(translation);
				alert.setHeaderText(string);
			}else{
				continue;
			}
			if (alert.getInformedEntityCount() > 0){
				alertIds.add(entity.getId());
				entity.setAlert(alert);
				update.addUpdatedEntity(entity.build());
			}
		}
		if (stations.containsKey(id)){
			ArrayList<String> oldAlertIds = stations.get(id);
			for (String oldId : oldAlertIds){
				boolean contains = false;
				for (String alertId : alertIds){
					if (alertId.equals(oldId)){
						contains = true;
					}
				}
				if (!contains){
					update.addDeletedEntity(oldId);
					continue;
				}
			}
		}else{
			stations.put(id, alertIds);
		}
		if (update.getDeletedEntities().size() > 0 || update.getUpdatedEntities().size() > 0){
			_alertsSink.handleIncrementalUpdate(update);
		}
	}

	private class ReceiveTask implements Runnable {
		int messagecounter = 0;
		@Override
		public void run() {
			int addressPointer = 0;
			Context context = ZMQ.context(1);
			Socket subscriber = context.socket(ZMQ.SUB);
			subscriber.connect(nsApiPublishers[addressPointer]);
			subscriber.subscribe("".getBytes());
			_log.info("Connected to {}",nsApiPublishers[addressPointer]);
			org.zeromq.ZMQ.Poller poller = context.poller();
			poller.register(subscriber);
			while (!Thread.interrupted()) {
				if (poller.poll(60*1000*5) > 0){
					messagecounter++;
					if (messagecounter % 1000 == 0){
						_log.debug(messagecounter + " NS-API messages received");
					}
					try {
						String[] m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						ArrayList<AVT> avt = AVT.fromCtx(m[1]);
						makeAlerts(m[0],avt);
					}catch (Exception e){
						_log.error("NS-API -> Alert",e);
					}
				}else{
					subscriber.disconnect(nsApiPublishers[addressPointer]);
					addressPointer++;
					if (addressPointer >= nsApiPublishers.length){
						addressPointer = 0;
					}
					_log.error("Connection to {} lost, reconnecting",nsApiPublishers[addressPointer]);
					subscriber.connect(nsApiPublishers[addressPointer]);
					subscriber.subscribe("".getBytes());
				}
			}
			subscriber.disconnect(nsApiPublishers[addressPointer]);
		}
	}
}
