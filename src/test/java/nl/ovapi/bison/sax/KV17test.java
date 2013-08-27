package nl.ovapi.bison.sax;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.ovapi.bison.model.KV17cvlinfo;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class KV17test {

	@Test
	public void test() throws ParserConfigurationException, SAXException,
	FileNotFoundException, IOException {
		test1();
		test2();
		test3();
		test4();
	}
	
	public void test4() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV17SAXHandler handler = new KV17SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv17-4.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		for (KV17cvlinfo cvlinfo: handler.getCvlinfos()){
			System.out.println(cvlinfo);
		}
	}

	public void test1() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV17SAXHandler handler = new KV17SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv17-1.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		if (handler.getCvlinfos().size() != 1)
			fail(handler.getCvlinfos().size() + " messages parsed instead of 1");
		if (handler.getCvlinfos().get(0).getMutations().size() != 18)
			fail(handler.getCvlinfos().get(0).getMutations().size()
					+ " messages parsed instead of 18");
	}

	public void test2() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV17SAXHandler handler = new KV17SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv17-2.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		if (handler.getCvlinfos().size() != 40)
			fail(handler.getCvlinfos().size() + " messages parsed instead of 40");
		if (handler.getCvlinfos().get(0).getMutations().size() != 1)
			fail(handler.getCvlinfos().get(0).getMutations().size()
					+ " messages parsed instead of 19");
	}

	public void test3() throws IOException, SAXException{
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp;
		XMLReader xr = null;
		try {sp = spf.newSAXParser();
		xr = sp.getXMLReader();} catch (Exception e) {return;}
		KV17SAXHandler handler = new KV17SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv17-3.xml");
		File f = new File(url.getFile());
	    BufferedReader reader = new BufferedReader( new FileReader (f));
	    StringBuilder stringBuilder = new StringBuilder();
	    for(String line = reader.readLine(); line != null; line = reader.readLine()) {
		    stringBuilder.append(line);
		    stringBuilder.append("\n");
		}
	    reader.close();
	    String msg = stringBuilder.toString();
		InputSource s = new InputSource(new StringReader(msg));
		xr.parse(s);
	}
}
