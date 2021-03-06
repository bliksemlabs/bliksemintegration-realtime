package nl.ovapi.rid.gtfsrt.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import nl.ovapi.bison.JourneyProcessor;
import nl.ovapi.bison.model.DataOwnerCode;
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

import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

public class KV17Test {
	@Before
	public void setUp() {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
		DateTimeZone.setDefault(DateTimeZone.forID("Europe/Amsterdam"));
	}

	public static JourneyPattern testPattern(){
		JourneyPattern.Builder jp = JourneyPattern.newBuilder();
		jp.setJourneyPatternref("403");
		jp.setDirectionType((byte)2);
		JourneyPatternPoint pt = JourneyPatternPoint.newBuilder()
				.setDistanceFromStartRoute(0)
				.setPointOrder((short)1)
				.setOperatorPointRef("10006900")
				.setPointRef(56858L)
				.setIsScheduled(true)
				.setIsWaitpoint(true).build();
		jp.add(pt);
		pt = JourneyPatternPoint.newBuilder()
				.setDistanceFromStartRoute(153)
				.setPointOrder((short)2)
				.setOperatorPointRef("10007110")
				.setPointRef(57796L)
				.setIsScheduled(true)
				.setIsWaitpoint(false).build();
		jp.add(pt);
		pt = JourneyPatternPoint.newBuilder()
				.setDistanceFromStartRoute(446)
				.setPointOrder((short)3)
				.setOperatorPointRef("10006780")
				.setPointRef(58494L)
				.setIsScheduled(true)
				.setIsWaitpoint(false).build();
		jp.add(pt);
		pt = JourneyPatternPoint.newBuilder()
				.setDistanceFromStartRoute(861)
				.setPointOrder((short)4)
				.setOperatorPointRef("10006450")
				.setPointRef(59619L)
				.setIsScheduled(true)
				.setIsWaitpoint(false).build();
		jp.add(pt);
		pt = JourneyPatternPoint.newBuilder()
				.setDistanceFromStartRoute(1087)
				.setPointOrder((short)5)
				.setOperatorPointRef("10006820")
				.setPointRef(59619L)
				.setIsScheduled(true)
				.setIsWaitpoint(false).build();
		jp.add(pt);
		pt = JourneyPatternPoint.newBuilder()
				.setDistanceFromStartRoute(1087)
				.setPointOrder((short)6)
				.setOperatorPointRef("10006450")
				.setPointRef(58636L)
				.setIsScheduled(true)
				.setIsWaitpoint(false).build();
		jp.add(pt);
		return jp.build();
	}
	
	public TimeDemandGroup testGroup(){
		TimeDemandGroup.Builder td = TimeDemandGroup.newBuilder();
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(0).setStopWaitTime(0).setPointOrder((short)1).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(60).setStopWaitTime(60).setPointOrder((short)2).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(120).setStopWaitTime(0).setPointOrder((short)3).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(180).setStopWaitTime(0).setPointOrder((short)4).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(240).setStopWaitTime(0).setPointOrder((short)5).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(300).setStopWaitTime(0).setPointOrder((short)6).build());
		return td.build();
	}

	public Journey getJourney(){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		Journey j = Journey.newBuilder()
			.setAgencyId("QBUZZ")
			.setDeparturetime(c.get(Calendar.HOUR_OF_DAY)*60*60+c.get(Calendar.MINUTE)*60+c.get(Calendar.SECOND))
			.setId("2552611")
			.setOperatingDay(df.format(c.getTime()))
			.setPrivateCode("QBUZZ:g005:1045")
			.setAvailabilityConditionRef(0L)
			.setJourneyPattern(testPattern())
			.setTimeDemandGroup(testGroup()).build();
		return j;
	}
	
	@Test
	public void cancel() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException, StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException {
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
			TripUpdate.Builder tripUpdate = j.update(cvlinfos).getGtfsRealtimeTrip();
			System.out.println(tripUpdate.build());
			assertTrue(tripUpdate.getTrip().getScheduleRelationship() == ScheduleRelationship.CANCELED);
		}
	}
	
	@Test
	public void shorten() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException, StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException {
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
			TripUpdate.Builder tripUpdate = j.update(cvlinfos).getGtfsRealtimeTrip();
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(0).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SCHEDULED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(2).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(3).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(4).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertEquals(tripUpdate.getStopTimeUpdateCount(),5);
		}
	}
	
	private static KV6posinfo testPosinfoArrival(){
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		posinfo.setOperatingday(format.format(new Date()));
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
	public void shortenMidTrip() throws ParserConfigurationException, SAXException, FileNotFoundException, IOException, StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException {
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
			TripUpdate.Builder tripUpdate = j.update(cvlinfos).getGtfsRealtimeTrip();
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(2).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(3).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertTrue(tripUpdate.getStopTimeUpdateBuilder(4).getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED);
			assertEquals(5,tripUpdate.getStopTimeUpdateCount());
		}
	}
}
