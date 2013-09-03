package nl.ovapi.rid.gtfsrt.services;

import static org.junit.Assert.*;

import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern;
import nl.ovapi.rid.model.TimeDemandGroup;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
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
		jp.points.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(2);
		pt.setOperatorpointref("10007110");
		pt.setPointref(57796L);
		pt.setDistancefromstartroute(153);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.points.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(3);
		pt.setOperatorpointref("10006780");
		pt.setPointref(58494L);
		pt.setDistancefromstartroute(446);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.points.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(4);
		pt.setOperatorpointref("10006450");
		pt.setPointref(59619L);
		pt.setDistancefromstartroute(861);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.points.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(5);
		pt.setOperatorpointref("10006820");
		pt.setPointref(58302L);
		pt.setDistancefromstartroute(1087);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.points.add(pt);
		pt = new JourneyPatternPoint();
		pt.setPointorder(6);
		pt.setOperatorpointref("10006450");
		pt.setPointref(58636L);
		pt.setDistancefromstartroute(1518);
		pt.setScheduled(true);
		pt.setWaitpoint(false);
		jp.points.add(pt);
		return jp;
	}
	
	public TimeDemandGroup testGroup(){
		TimeDemandGroup td = new TimeDemandGroup();
		TimeDemandGroupPoint tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(0);
		tp.setStopwaittime(0);
		tp.setPointorder(1);
		td.points.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(60);
		tp.setStopwaittime(0);
		tp.setPointorder(2);
		td.points.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(120);
		tp.setStopwaittime(60);
		tp.setPointorder(3);
		td.points.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(240);
		tp.setStopwaittime(0);
		tp.setPointorder(4);
		td.points.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(300);
		tp.setStopwaittime(0);
		tp.setPointorder(5);
		td.points.add(tp);
		tp = new TimeDemandGroupPoint();
		tp.setTotaldrivetime(360);
		tp.setStopwaittime(0);
		tp.setPointorder(6);
		td.points.add(tp);
		return td;

	}

	public Journey getJourney(){
		Journey j = new Journey();
		j.setAgencyId("QBUZZ");
		j.setDeparturetime(50280);
		j.setId(2552611L);
		j.setOperatingDay("2013-08-27");
		j.setJourneypattern(testPattern());
		j.setTimedemandgroup(testGroup());
		return j;
	}
	@Test
	public void testNegativeOnFirstStopAsTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException{
		Journey j = getJourney();
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday("2013-08-27");
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode("10006900");
		posinfo.setPunctuality(-120);
		posinfo.setTimestamp(j.getDepartureEpoch()-120);
		posinfo.setPassagesequencenumber(-120);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertTrue(tripUpdate.getStopTimeUpdateCount() == 2);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertFalse(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),1377604560);
	}
	
	@Test
	public void testNegativeOnFirstStopNotAsTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException{
		Journey j = getJourney();
		j.getJourneypattern().getPoint(1).setWaitpoint(false);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday("2013-08-27");
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode("10006900");
		posinfo.setPunctuality(-120);
		posinfo.setTimestamp(j.getDepartureEpoch()-120);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertTrue(tripUpdate.getStopTimeUpdateCount() == 2);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertFalse(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),1377604560);
	}
	@Test
	public void testNegativeOnTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException{
		Journey j = getJourney();
		j.getJourneypattern().getPoint(3).setWaitpoint(true);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday("2013-08-27");
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode(j.getJourneypattern().getPoint(3).getOperatorpointref());
		posinfo.setPunctuality(-120);
		posinfo.setTimestamp(j.getDepartureEpoch());
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertTrue(tripUpdate.getStopTimeUpdateCount() == 2);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertFalse(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),j.getDepartureEpoch());
		assertEquals(tripUpdate.getStopTimeUpdate(1).getArrival().getTime(),1377604920);
		assertFalse(tripUpdate.getStopTimeUpdate(1).hasDeparture());
	}
	
	@Test
	public void testNegativeOnDepartureFirstStop() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException{
		Journey j = getJourney();
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday("2013-08-27");
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.DEPARTURE);
		posinfo.setUserstopcode("10006900");
		posinfo.setPunctuality(-120);
		posinfo.setTimestamp(j.getDepartureEpoch()-120);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo);
		assertTrue(tripUpdate.getStopTimeUpdateCount() == 1);
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),j.getDepartureEpoch()+60);
	}
}
