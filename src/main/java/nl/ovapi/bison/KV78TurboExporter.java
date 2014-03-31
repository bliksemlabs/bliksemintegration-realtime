package nl.ovapi.bison;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import nl.ovapi.bison.model.DatedPasstime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Queues;


@Singleton
public class KV78TurboExporter {

	private static final Logger _log = LoggerFactory.getLogger(KV78TurboExporter.class);
	private ScheduledExecutorService _scheduler;
	private ConcurrentLinkedQueue<List<DatedPasstime>> workQueue;

	@PostConstruct
	public void start() {
		_scheduler = Executors.newScheduledThreadPool(1);
		_scheduler.scheduleAtFixedRate(new SendTask(), 500, 500, TimeUnit.MILLISECONDS);
		workQueue = Queues.newConcurrentLinkedQueue();
	}

	private class SendTask implements Runnable{
		@Override
		public void run() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < 500; i++){
				List<DatedPasstime> passtimes = workQueue.poll();
				if (passtimes == null){
					break;
				}
				for (DatedPasstime dp : passtimes){
					sb.append(dp.toCtxLine()).append('\n');
				}
				sb.append('\n');
			}
			if (sb.length() > 0)
				System.out.println(sb);
		}
	}

	public void export(List<DatedPasstime> datedPasstimes){
		if (datedPasstimes != null && datedPasstimes.size() > 0)
			workQueue.offer(datedPasstimes);
	}
}
