package nl.ovapi.rid.gtfsrt.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.JourneyProcessor;
import nl.ovapi.bison.model.KV17cvlinfo;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.bison.sax.KV17SAXHandler;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.TooOldException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiStopTimeUpdate;

public class KV17Test {

	public static JourneyPattern testPattern(){
		JourneyPattern jp = new JourneyPattern();
		jp.setJourneyPatternRef("403");
		jp.setDirectiontype(2);
		JourneyPatternPoint pt = new JourneyPatternPoint();
		pt.setDistancefromstartroute(0);
		pt.setPointorder(1);
		pt.setOperatorpointref("10006900");
		pt.setPointref(56858L);
		pt.setDistancefromstartroute(0);
		pt.setScheduled(true);
		pt.setWaitpoint(true);
		jp.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(2);
		pt.setOperatorpointref("10007110");
		pt.setPointref(57796L);
		pt.setDistancefromstartroute(153);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(3);
		pt.setOperatorpointref("10006780");
		pt.setPointref(58494L);
		pt.setDistancefromstartroute(446);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(4);
		pt.setOperatorpointref("10006450");
		pt.setPointref(59619L);
		pt.setDistancefromstartroute(861);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(5);
		pt.setOperatorpointref("10006820");
		pt.setPointref(58302L);
		pt.setDistancefromstartroute(1087);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(6);
		pt.setOperatorpointref("10006450");
		pt.setPointref(58636L);
		pt.setDistancefromstartroute(1518);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.add(pt);
		return jp;
	}
	
	public TimeDemandGroup testGroup(){
		TimeDemandGroup td = new TimeDemandGroup();
		TimeDemandGroupPoint tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(0);
		tp.setStopwaittime(0);
		tp.setPointorder(1);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(60);
		tp.setStopwaittime(60);
		tp.setPointorder(2);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(120);
		tp.setStopwaittime(120);
		tp.setPointorder(3);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(180);
		tp.setStopwaittime(180);
		tp.setPointorder(4);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(240);
		tp.setStopwaittime(240);
		tp.setPointorder(5);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(300);
		tp.setStopwaittime(300);
		tp.setPointorder(6);
		td.add(tp);
		return td;

	}

	public Journey getJourney(){
		Journey j = new Journey();
		j.setAgencyId("QBUZZ");
		Calendar c = Calendar.getInstance();
		j.setDeparturetime(c.get(Calendar.HOUR_OF_DAY)*60*60+c.get(Calendar.MINUTE)*60+c.get(Calendar.SECOND));
		j.setId(2552611L+"");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		j.setOperatingDay(df.format(c.getTime()));
		j.setJourneypattern(testPattern());
		j.setAvailabilityConditionRef(0L);
		j.setPrivateCode("QBUZZ:g005:1045");
		j.setTimedemandgroup(testGroup());
		return j;
	}
	
	@Test
	public void cancel() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException {
		Journey journey = getJourney();
		JourneyProcessor j = new JourneyProcessor(journey);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV17SAXHandler handler = new KV17SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv17cancel.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		ArrayList<KV17cvlinfo> cvlinfos = handler.getCvlinfos();
		HashMap<String,ArrayList<KV17cvlinfo>> map = new HashMap<String,ArrayList<KV17cvlinfo>>();
		for (KV17cvlinfo cvlinfo : cvlinfos){
			String id = String.format("%s:%s:%s:%s", cvlinfo.getOperatingday(),cvlinfo.getDataownercode().name(),cvlinfo.getLineplanningnumber(),cvlinfo.getJourneynumber());
			if (!map.containsKey(id)){
				map.put(id, new ArrayList<KV17cvlinfo>());
			}
			map.get(id).add(cvlinfo);
		}
		assertTrue(map.keySet().size() == 1);
		for (String id : map.keySet()){
			cvlinfos = map.get(id);
			TripUpdate.Builder tripUpdate = j.update(cvlinfos);
			assertTrue(journey.isCanceled());
			assertTrue(tripUpdate.getTrip().getScheduleRelationship() == ScheduleRelationship.CANCELED);
		}
	}
	
	@Test
	public void shorten() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException {
		Journey journey = getJourney();
		JourneyProcessor j = new JourneyProcessor(journey);
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV17SAXHandler handler = new KV17SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv17shorten.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		ArrayList<KV17cvlinfo> cvlinfos = handler.getCvlinfos();
		HashMap<String,ArrayList<KV17cvlinfo>> map = new HashMap<String,ArrayList<KV17cvlinfo>>();
		for (KV17cvlinfo cvlinfo : cvlinfos){
			String id = String.format("%s:%s:%s:%s", cvlinfo.getOperatingday(),cvlinfo.getDataownercode().name(),cvlinfo.getLineplanningnumber(),cvlinfo.getJourneynumber());
			if (!map.containsKey(id)){
				map.put(id, new ArrayList<KV17cvlinfo>());
			}
			map.get(id).add(cvlinfo);
		}
		assertTrue(map.keySet().size() == 1);
		for (String id : map.keySet()){
			cvlinfos = map.get(id);
			TripUpdate.Builder tripUpdate = j.update(cvlinfos);
			assertTrue(j.hasMutations());
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(0).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SCHEDULED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(0).hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate));
			OVapiStopTimeUpdate extension = tripUpdate.getStopTimeUpdateBuilder(0).getExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
			assertEquals(extension.getStopHeadsign(),"Arnhem Centraalspoor");
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(2).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(3).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(4).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertEquals(tripUpdate.getStopTimeUpdateCount(),6);
		}
	}
	
	private static KV6posinfo testPosinfoArrival(){
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday("2013-08-27");
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode("10006450");
		posinfo.setPunctuality(0);
		posinfo.setTimestamp(Utils.currentTimeSecs());
		posinfo.setPassagesequencenumber(0);
		return posinfo;
	}
	@Test
	public void shortenMidTrip() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException, StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException {
		Journey journey = getJourney();
		JourneyProcessor j = new JourneyProcessor(journey);
		j.update(testPosinfoArrival());
		SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setNamespaceAware(true);
		SAXParser sp = spf.newSAXParser();
		XMLReader xr = sp.getXMLReader();
		KV17SAXHandler handler = new KV17SAXHandler();
		xr.setContentHandler(handler);
		URL url = this.getClass().getResource("kv17shorten.xml");
		File f = new File(url.getFile());
		xr.parse(new InputSource(new FileInputStream(f)));
		ArrayList<KV17cvlinfo> cvlinfos = handler.getCvlinfos();
		HashMap<String,ArrayList<KV17cvlinfo>> map = new HashMap<String,ArrayList<KV17cvlinfo>>();
		for (KV17cvlinfo cvlinfo : cvlinfos){
			String id = String.format("%s:%s:%s:%s", cvlinfo.getOperatingday(),cvlinfo.getDataownercode().name(),cvlinfo.getLineplanningnumber(),cvlinfo.getJourneynumber());
			if (!map.containsKey(id)){
				map.put(id, new ArrayList<KV17cvlinfo>());
			}
			map.get(id).add(cvlinfo);
		}
		assertTrue(map.keySet().size() == 1);
		for (String id : map.keySet()){
			cvlinfos = map.get(id);
			TripUpdate.Builder tripUpdate = j.update(cvlinfos);
			assertTrue(j.hasMutations());
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(0).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SCHEDULED);			assertTrue(tripUpdate.getStopTimeUpdateBuilder(0).hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate));
			OVapiStopTimeUpdate extension = tripUpdate.getStopTimeUpdateBuilder(0).getExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
			assertEquals(extension.getStopHeadsign(),"Arnhem Centraalspoor");
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(2).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(3).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(4).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(5).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SCHEDULED);
			assertTrue(tripUpdate.getStopTimeUpdateCount() == 6);
		}
	}
}
