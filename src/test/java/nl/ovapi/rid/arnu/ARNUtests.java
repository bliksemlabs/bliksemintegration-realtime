package nl.ovapi.rid.arnu;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import nl.ovapi.arnu.TrainProcessor;
import nl.tt_solutions.schemas.ns.rti._1.PutServiceInfoIn;

import org.junit.Test;

public class ARNUtests {

	@Test
	public void testOriginalTrainNumber(){
		assertEquals(TrainProcessor.orginalTrainNumber("305622"),Integer.valueOf(5622));
		assertEquals(TrainProcessor.orginalTrainNumber("315622"),Integer.valueOf(5622));
		assertEquals(TrainProcessor.orginalTrainNumber("325622"),Integer.valueOf(5622));
		assertEquals(TrainProcessor.orginalTrainNumber("300839"),Integer.valueOf(839));
	}

	@Test
	public void addAndExtend() throws JAXBException, FileNotFoundException{
		JAXBContext jc = JAXBContext.newInstance(PutServiceInfoIn.class);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		URL url = this.getClass().getResource("309674_added_1.xml");
		File f = new File(url.getFile());
		FileInputStream stream = new FileInputStream(f);
		JAXBElement<PutServiceInfoIn> feed = unmarshaller.unmarshal(new StreamSource(stream), PutServiceInfoIn.class);
	}
}
