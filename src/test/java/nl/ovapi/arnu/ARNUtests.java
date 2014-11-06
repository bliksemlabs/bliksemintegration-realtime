package nl.ovapi.arnu;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.text.ParseException;
import java.util.TimeZone;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import nl.ovapi.arnu.TrainProcessor;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.tt_solutions.schemas.ns.rti._1.PutServiceInfoIn;

import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

public class ARNUtests {
	
	private RIDservice _ridService;
	
	public ARNUtests(){
		_ridService = new FakeRidService();
	}

	@Before
	public void setUp() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
		DateTimeZone.setDefault(DateTimeZone.forID("Europe/Amsterdam"));
	}
	
	@Test
	public void testOriginalTrainNumber(){
		assertEquals(TrainProcessor.orginalTrainNumber("305622"),Integer.valueOf(5622));
		assertEquals(TrainProcessor.orginalTrainNumber("315622"),Integer.valueOf(5622));
		assertEquals(TrainProcessor.orginalTrainNumber("325622"),Integer.valueOf(5622));
		assertEquals(TrainProcessor.orginalTrainNumber("300839"),Integer.valueOf(839));
	}

	@Test
	public void addAndExtend() throws JAXBException, FileNotFoundException, ParseException{
		JAXBContext jc = JAXBContext.newInstance(PutServiceInfoIn.class);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		URL url = this.getClass().getResource("309674_added_1.xml");
		File f = new File(url.getFile());
		FileInputStream stream = new FileInputStream(f);
		JAXBElement<PutServiceInfoIn> feed = unmarshaller.unmarshal(new StreamSource(stream), PutServiceInfoIn.class);
		TrainProcessor jp = TrainProcessor.fromArnu(_ridService,feed.getValue().getServiceInfoList().getServiceInfo().get(0));
		assertEquals(1,jp.get_processors().size());
		assertEquals(1394654760,jp.get_processors().get(0).getBlock().getDepartureEpoch());
		assertEquals(1,jp.get_processors().get(0).getBlock().getSegments().size());
		assertEquals(1394655540,jp.get_processors().get(0).getBlock().getEndEpoch());
		assertEquals(3,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().size());
		assertEquals(1L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(0).getPointref());
		assertEquals(2L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(1).getPointref());
		assertEquals(3L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(2).getPointref());
		
		url = this.getClass().getResource("309674_added_2.xml");
		f = new File(url.getFile());
		stream = new FileInputStream(f);
		feed = unmarshaller.unmarshal(new StreamSource(stream), PutServiceInfoIn.class);
		
		jp.changeService(_ridService, feed.getValue().getServiceInfoList().getServiceInfo().get(0));
		
		assertEquals(1,jp.get_processors().size());
		assertEquals(1394654040,jp.get_processors().get(0).getBlock().getDepartureEpoch());
		assertEquals(1,jp.get_processors().get(0).getBlock().getSegments().size());
		assertEquals(1394655540,jp.get_processors().get(0).getBlock().getEndEpoch());
		assertEquals(5,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().size());
		assertEquals(4L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(0).getPointref());
		assertEquals(5L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(1).getPointref());
		assertEquals(1L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(2).getPointref());
		assertEquals(2L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(3).getPointref());
		assertEquals(3L,jp.get_processors().get(0).getBlock().getSegments().get(0).getJourneypattern().getPoints().get(4).getPointref());
	}
}
