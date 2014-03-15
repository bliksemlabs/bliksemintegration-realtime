package nl.ovapi.rid.gtfsrt.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.JourneyProcessor;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.TooOldException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import org.junit.Test;

import com.google.transit.realtime.GtfsRealtime.TripUpdate;

public class KV6Test {

	public static JourneyPattern testPattern(){
		JourneyPattern jp = new JourneyPattern();
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
		tp.setStopwaittime(0);
		tp.setPointorder(2);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(120);
		tp.setStopwaittime(60);
		tp.setPointorder(3);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(240);
		tp.setStopwaittime(0);
		tp.setPointorder(4);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(300);
		tp.setStopwaittime(0);
		tp.setPointorder(5);
		td.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(360);
		tp.setStopwaittime(0);
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
		j.setTimedemandgroup(testGroup());
		j.setPrivateCode("QBUZZ:g005:1045");
		return j;
	}
	@Test
	public void testNegativeOnFirstStopAsTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException{
		Journey journey = getJourney();
		JourneyProcessor j = new JourneyProcessor(journey);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday(journey.getOperatingDay());
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode("10006900");
		posinfo.setPunctuality(-120);
		posinfo.setTimestamp(journey.getDepartureEpoch()-60);
		posinfo.setPassagesequencenumber(-120);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertTrue(tripUpdate.getStopTimeUpdateCount() == 1);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),journey.getDepartureEpoch()-60);
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getDelay(),-60);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getTime(),journey.getDepartureEpoch());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getDelay(),0);

	}
	
	@Test
	public void testNegativeOnFirstStopNotAsTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException{
		Journey journey = getJourney();
		JourneyProcessor j = new JourneyProcessor(journey);
		journey.getJourneypattern().getPoint(1).setWaitpoint(false);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday(journey.getOperatingDay());
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode("10006900");
		posinfo.setPunctuality(-120);
		posinfo.setTimestamp(journey.getDepartureEpoch()-120);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertTrue(tripUpdate.getStopTimeUpdateCount() == 1);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),journey.getDepartureEpoch()-120);
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getDelay(),-120);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getTime(),journey.getDepartureEpoch());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getDelay(),0);

	}
	@Test
	public void testNegativeOnTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException{
		Journey journey = getJourney();
		JourneyProcessor j = new JourneyProcessor(journey);
		journey.getJourneypattern().getPoint(3).setWaitpoint(true);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday(journey.getOperatingDay());
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode(journey.getJourneypattern().getPoint(3).getOperatorpointref());
		posinfo.setPunctuality(-30);
		posinfo.setTimestamp(journey.getDepartureEpoch()+100);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertEquals(2,tripUpdate.getStopTimeUpdateCount());
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getDelay(),0);
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getTime(),journey.getDepartureEpoch());
		assertEquals(tripUpdate.getStopTimeUpdate(1).getArrival().getDelay(),-20);
		assertEquals(tripUpdate.getStopTimeUpdate(1).getDeparture().getDelay(),0);
	}
	
	@Test
	public void testNegativeOnDepartureFirstStop() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException{
		Journey journey = getJourney();
		JourneyProcessor j = new JourneyProcessor(journey);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday(journey.getOperatingDay());
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.DEPARTURE);
		posinfo.setUserstopcode("10006900");
		posinfo.setPunctuality(-120);
		posinfo.setTimestamp(journey.getDepartureEpoch()-120);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertEquals(1,tripUpdate.getStopTimeUpdateCount());
		assertFalse(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getDelay(),0);
	}
}
