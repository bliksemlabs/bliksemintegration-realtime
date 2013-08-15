package nl.ovapi.rid.gtfsrt.services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.DatedPasstime;
import nl.ovapi.bison.model.JourneyStopType;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.bison.model.TripStopStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

@Singleton
public class KV78TurboToPseudoKV6Service {

	private BisonToGtfsRealtimeService _bisonToGtfsRealtimeService;
	private ExecutorService _executor;
	private Future<?> _task;
	private static final Logger _log = LoggerFactory.getLogger(KV78TurboToPseudoKV6Service.class);
	private ScheduledExecutorService _scheduler;
	private final static String[] kv8turboPublishers = new String[] {"tcp://post.ndovloket.nl:7817"};

	private HashMap<String,DatedPasstime> livePasstimes;

	@Inject
	public void setBisonToGtfsRealtimeService(BisonToGtfsRealtimeService bisonToGtfsRealtimeService) {
		_bisonToGtfsRealtimeService = bisonToGtfsRealtimeService;
	}

	@PostConstruct
	public void start() {
		livePasstimes = new HashMap<String,DatedPasstime>();
		_executor = Executors.newCachedThreadPool();
		_scheduler = Executors.newScheduledThreadPool(5);
		_scheduler.scheduleAtFixedRate(new RefreshTask(), 60, 10, TimeUnit.SECONDS);
		_task = _executor.submit(new ReceiveTask());
	}

	private class RefreshTask implements Runnable{
		@Override
		public void run() {
			long threshold = System.currentTimeMillis() - (1000 * 90);
			ArrayList<String> removeIds = new ArrayList<String>();
			ArrayList<KV6posinfo> refreshedPosinfos = new ArrayList<KV6posinfo>();
			for (String key : new HashSet<String>(livePasstimes.keySet())){
				try{
					DatedPasstime pt = livePasstimes.get(key);
					if (pt.getLastUpdateTimeStamp() < threshold){
						if (pt.getJourneyStopType() != JourneyStopType.LAST){
							if (pt.getTripStopStatus() != TripStopStatus.PLANNED && pt.getTripStopStatus() != TripStopStatus.UNKNOWN && pt.getTripStopStatus() != TripStopStatus.CANCEL){
								SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
								long departureEpoch = df.parse(pt.getOperationDate()).getTime() + (1000*pt.getExpectedArrivalTime());
								if (System.currentTimeMillis() < departureEpoch){
									pt.setLastUpdateTimeStamp(System.currentTimeMillis());
									refreshedPosinfos.add(makePseudoKV6(pt));
								}
							}
						}
					}
				}catch (Exception e){

				}
			}
			if (refreshedPosinfos.size() > 0)
				_bisonToGtfsRealtimeService.process(refreshedPosinfos);
			if (removeIds.size() > 0)
				_bisonToGtfsRealtimeService.remove(removeIds);
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

	private KV6posinfo makePseudoKV6(DatedPasstime pt){
		String id = String.format("%s:%s:%s:%s", pt.getOperationDate(),pt.getDataOwnerCode(),pt.getLinePlanningNumber(),pt.getJourneyNumber());
		KV6posinfo posinfo = new KV6posinfo();
		switch (pt.getTripStopStatus()){
		case ARRIVED:
			posinfo.setMessagetype(Type.ONROUTE);
			break;
		case DRIVING:
			if (pt.getJourneyStopType() == JourneyStopType.FIRST)
				posinfo.setMessagetype(Type.DELAY);
			else
				posinfo.setMessagetype(Type.ONROUTE);
		case OFFROUTE:
			break;
		case PASSED:
			posinfo.setMessagetype(Type.DEPARTURE);
		case UNKNOWN:
			return null;
		case PLANNED:
		case CANCEL:
			_log.info("Cancel, damn lazy slumb implement this {} {}",pt.getTripStopStatus(),id);
			return null;
		default:
			break;
		}
		posinfo.setOperatingday(pt.getOperationDate());
		posinfo.setReinforcementnumber(pt.getFortifyOrderNumber());
		posinfo.setTimestamp(pt.getLastUpdateTimeStamp());
		posinfo.setRd_x(-1);
		posinfo.setRd_y(-1);
		if (pt.getDataOwnerCode() == DataOwnerCode.GVB){
			String stopCode = pt.getUserStopCode();
			posinfo.setUserstopcode(stopCode.substring(0,stopCode.length()-1));
		}else{
			posinfo.setUserstopcode(pt.getUserStopCode());
		}
		posinfo.setDataownercode(pt.getDataOwnerCode());
		posinfo.setLineplanningnumber(pt.getLinePlanningNumber());
		posinfo.setJourneynumber(pt.getJourneyNumber());
		posinfo.setPunctuality(pt.getTargetDepartureTime()-pt.getExpectedArrivalTime());
		posinfo.setWheelchairaccessible(pt.getWheelChairAccessible());
		posinfo.setNumberofcoaches(posinfo.getNumberofcoaches());
		posinfo.setPassagesequencenumber(0);
		return posinfo;
	}

	private class ReceiveTask implements Runnable {
		int messagecounter = 0;
		@Override
		public void run() {
			int addressPointer = 0;
			Context context = ZMQ.context(1);
			Socket subscriber = context.socket(ZMQ.XSUB);
			subscriber.connect(kv8turboPublishers[addressPointer]);
			char enable = 0x01;
			subscriber.send(enable+"/GOVI/KV8");
			_log.info("Connected to {}",kv8turboPublishers[addressPointer]);
			org.zeromq.ZMQ.Poller poller = context.poller();
			poller.register(subscriber);
			while (true) {
				if (poller.poll(60*1000*5) > 0){
					messagecounter++;
					if (messagecounter % 1000 == 0){
						_log.debug(messagecounter + " KV8Turbo messages received");
					}
					try {
						String[] m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						HashMap<String,DatedPasstime> passtimes = new HashMap<String,DatedPasstime>();
						for (DatedPasstime pt : DatedPasstime.fromCtx(m[1])){
							if (pt.getJourneyStopType() == JourneyStopType.INFOPOINT)
								continue;
							switch (pt.getDataOwnerCode()){
							case GVB:
							case EBS:
							case SYNTUS:
								break;
							default:
								continue;
							}
							String id = String.format("%s:%s:%s:%s", pt.getOperationDate(),pt.getDataOwnerCode(),pt.getLinePlanningNumber(),pt.getJourneyNumber());
							if (!passtimes.containsKey(id) || pt.getUserStopOrderNumber() < passtimes.get(id).getUserStopOrderNumber()){
								passtimes.put(id, pt);
							}
						}
						ArrayList<KV6posinfo> posinfos = new ArrayList<KV6posinfo>();
						ArrayList<String> removeIds = new ArrayList<String>();
						for (DatedPasstime pt : passtimes.values()){
							String id = String.format("%s:%s:%s:%s", pt.getOperationDate(),pt.getDataOwnerCode(),pt.getLinePlanningNumber(),pt.getJourneyNumber());
							if (pt.getTripStopStatus() == TripStopStatus.UNKNOWN){
								if (livePasstimes.containsKey(id)){
									livePasstimes.remove(id);
									removeIds.add(id);
								}
							}
							KV6posinfo posinfo = makePseudoKV6(pt);
							if (posinfo != null){
								posinfos.add(posinfo);
								livePasstimes.put(id, pt);
							}
						}
						_bisonToGtfsRealtimeService.process(posinfos);
						_bisonToGtfsRealtimeService.remove(removeIds);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}else{
					subscriber.disconnect(kv8turboPublishers[addressPointer]);
					addressPointer++;
					if (addressPointer >= kv8turboPublishers.length){
						addressPointer = 0;
					}
					_log.error("Connection to {} lost, reconnecting",kv8turboPublishers[addressPointer]);
					subscriber.connect(kv8turboPublishers[addressPointer]);
					subscriber.subscribe("".getBytes());
				}
			}
		}
	}
}
