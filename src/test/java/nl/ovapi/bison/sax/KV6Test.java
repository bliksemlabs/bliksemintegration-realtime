package nl.ovapi.bison.sax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class KV6Test {

    @Test
    public void test1() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);
        SAXParser sp = spf.newSAXParser();
        XMLReader xr = sp.getXMLReader();
        KV6SAXHandler handler = new KV6SAXHandler();
        xr.setContentHandler(handler);
        URL url = this.getClass().getResource("kv6-3.xml");
        File f = new File(url.getFile());
        xr.parse(new InputSource(new FileInputStream(f)));
        ArrayList<KV6posinfo> posinfos = handler.getPosinfos();
        assertTrue(posinfos.size() == 3);
        KV6posinfo posinfo = posinfos.get(0);
        assertTrue(posinfo.getMessagetype() == Type.DEPARTURE);
        assertTrue(posinfo.getPunctuality() == 205);
        assertEquals(1375802845L, posinfo.getTimestamp().longValue());
    }
}
