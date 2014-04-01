package nl.ovapi.bison;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import nl.ovapi.bison.model.DatedPasstime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.google.common.collect.Queues;


@Singleton
public class KV78TurboExporter {

	private static final Logger _log = LoggerFactory.getLogger(KV78TurboExporter.class);
	private ScheduledExecutorService _scheduler;
	private ConcurrentLinkedQueue<List<DatedPasstime>> workQueue;
	private Context context;
	private Socket publisher;

	@PostConstruct
	public void start() {
		_scheduler = Executors.newScheduledThreadPool(1);
		_scheduler.scheduleAtFixedRate(new SendTask(), 100, 100, TimeUnit.MILLISECONDS);
		workQueue = Queues.newConcurrentLinkedQueue();
		context = ZMQ.context(1);
		publisher = context.socket(ZMQ.XPUB);
		publisher.bind("tcp://0.0.0.0:7817");
	}

	private class SendTask implements Runnable{
		@Override
		public void run() {
			try{
				StringBuilder sb = new StringBuilder();
				sb.append(DatedPasstime.header("OVapi_KV8"));
				for (int i = 0; i < 500; i++){
					List<DatedPasstime> passtimes = workQueue.poll();
					if (passtimes == null){
						break;
					}
					for (DatedPasstime dp : passtimes){
						sb.append(dp.toCtxLine()).append("\r\n");
					}
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(out);
				gzip.write(sb.toString().getBytes("UTF-8"));
				gzip.close();
				publisher.sendMore("/GOVI/KV8passtimes");
				publisher.send(out.toByteArray());
				out.close();
			}catch (Exception e){
				_log.error("Sending KV8 fail",e);
			}
		}
	}

	public void export(List<DatedPasstime> datedPasstimes){
		if (datedPasstimes != null && datedPasstimes.size() > 0)
			workQueue.offer(datedPasstimes);
	}
}
