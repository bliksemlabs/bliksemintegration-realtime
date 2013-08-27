package nl.ovapi.bison.sax;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.ovapi.bison.model.KV15message;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class KV15Test {

	@Test
	public void test() throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		test1();
		test2();
	}

	public void test2()  throws ParserConfigurationException, SAXException, FileNotFoundException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV15SAXHandler handler = new KV15SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv15-1.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		ArrayList<KV15message> messages = handler.getMessages();
		assertEquals(messages.size(),1);
		assertFalse(messages.get(0).getIsDelete().booleanValue());
		assertNotNull(messages.get(0).getMessageContent());
	}
	
	public void test1()  throws ParserConfigurationException, SAXException, FileNotFoundException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV15SAXHandler handler = new KV15SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv15.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		ArrayList<KV15message> messages = handler.getMessages();
		assertEquals(messages.size(), 2);
		assertTrue(messages.get(0).getIsDelete().booleanValue());
		assertFalse(messages.get(1).getIsDelete().booleanValue());
		assertNotNull(messages.get(1).getMessageContent());
	}
}
