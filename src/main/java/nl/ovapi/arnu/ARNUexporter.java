package nl.ovapi.arnu;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import nl.tt_solutions.schemas.ns.rti._1.PutServiceInfoIn;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceList;
import nl.tt_solutions.schemas.ns.rti._1.ServiceInfoServiceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.google.common.collect.Queues;


@Singleton
public class ARNUexporter {

	private static final Logger _log = LoggerFactory.getLogger(ARNUexporter.class);
	private ScheduledExecutorService _scheduler;
	private ConcurrentLinkedQueue<ServiceInfoServiceType> workQueue;
	private Socket publisher;
	private Context context;
	private static boolean FORMAT_XML = true;

	@PostConstruct
	public void start() {
		_scheduler = Executors.newScheduledThreadPool(1);
		_scheduler.scheduleAtFixedRate(new SendTask(), 500, 500, TimeUnit.MILLISECONDS);
		workQueue = Queues.newConcurrentLinkedQueue();
		context = ZMQ.context(1);
		publisher = context.socket(ZMQ.XPUB);
		publisher.bind("tcp://0.0.0.0:9817");
	}

	private class SendTask implements Runnable{
		@Override
		public void run() {
			try {
				PutServiceInfoIn putService = new PutServiceInfoIn();
				ServiceInfoServiceList infoList = new ServiceInfoServiceList();
				putService.setServiceInfoList(infoList);
				infoList.setInitial(false);
				JAXBContext jc = null;
				for (int i = 0; i < 500; i++){
					ServiceInfoServiceType serviceInfo = workQueue.poll();
					if (serviceInfo == null){
						break;
					}
					infoList.getServiceInfo().add(serviceInfo);
				}
				if (infoList.getServiceInfo().size() == 0){
					return;
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(out);
				Marshaller marshaller = null;
				try {
					jc = JAXBContext.newInstance(PutServiceInfoIn.class);
					marshaller = jc.createMarshaller();
					marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, FORMAT_XML);
				} catch (JAXBException e1) {
					_log.error("Error with JAXB",e1);
					e1.printStackTrace();
				}
				marshaller.marshal(putService, gzip);
				gzip.close();
				publisher.sendMore("/OVAPI/ARNURitinfo");
				publisher.send(out.toByteArray());
				out.close();
				gzip.close();
			} catch (Exception e) {
				_log.error("ARNU exporter exception",e);
				e.printStackTrace();
			}	
		}
	}

	public void export(ServiceInfoServiceType serviceInfo){
		workQueue.offer(serviceInfo);
	}
}
