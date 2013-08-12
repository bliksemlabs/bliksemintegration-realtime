package nl.ovapi.rid.gtfsrt.services;

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

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeSink;
import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeGuiceBindingTypes.TripUpdates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

@Singleton
public class NSapiToGtfsRealTimeServices {

	private ExecutorService _executor;
	private Future<?> _task;
	private static final Logger _log = LoggerFactory.getLogger(NSapiToGtfsRealTimeServices.class);
	private ScheduledExecutorService _scheduler;
	private final static String[] nsApiPublishers = new String[] {"tcp://node01.post.openov.nl:6611"};
	private GtfsRealtimeSink _tripUpdatesSink;
	
	@Inject
	public void setTripUpdatesSink(@TripUpdates	GtfsRealtimeSink tripUpdatesSink) {
		_tripUpdatesSink = tripUpdatesSink;
	}
		
	@PostConstruct
	public void start() {
		_executor = Executors.newCachedThreadPool();
		_scheduler = Executors.newScheduledThreadPool(5);
		_scheduler.scheduleAtFixedRate(new CleanTask(), 60, 10, TimeUnit.SECONDS);
		_task = _executor.submit(new ReceiveTask());
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
		if (_scheduler != null) {
			_scheduler.shutdownNow();
			_scheduler = null;
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
			while (true) {
				if (poller.poll(60000) > 0){
					messagecounter++;
					if (messagecounter % 1000 == 0){
						_log.debug(messagecounter + " NS-API messages received");
					}
					try {
						String[] m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						m = ZeroMQUtils.gunzipMultifameZMsg(ZMsg.recvMsg(subscriber));
						System.out.println(m[1]);
					}catch (Exception e){}
				}else{
					addressPointer++;
					if (addressPointer >= nsApiPublishers.length){
						addressPointer = 0;
					}
					_log.error("Connection to {} lost, reconnecting",nsApiPublishers[addressPointer]);
					subscriber.connect(nsApiPublishers[addressPointer]);
					subscriber.subscribe("".getBytes());
				}
			}
		}
	}
}
