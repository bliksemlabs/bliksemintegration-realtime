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

import nl.ovapi.bison.model.KV6posinfo;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class KV6test {

	@Test
	public void test() throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
		test1();
	}

	public void test1()  throws ParserConfigurationException, SAXException, FileNotFoundException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV6SAXHandler handler = new KV6SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv6-3.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		for (KV6posinfo posinfo : handler.getPosinfos())
			System.out.println(posinfo);
	}
}
