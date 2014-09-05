package nl.ovapi.rid.gtfsrt.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import nl.ovapi.bison.JourneyProcessor;
import nl.ovapi.bison.model.DataOwnerCode;
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
	
	public static JourneyPattern testPattern3(){
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
				.setIsWaitpoint(true).build();
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

	public static JourneyPattern testPattern2(){
		JourneyPattern.Builder jp = JourneyPattern.newBuilder();
		jp.setJourneyPatternref("403");
		jp.setDirectionType((byte)2);
		JourneyPatternPoint pt = JourneyPatternPoint.newBuilder()
				.setDistanceFromStartRoute(0)
				.setPointOrder((short)1)
				.setOperatorPointRef("10006900")
				.setPointRef(56858L)
				.setIsScheduled(true)
				.setIsWaitpoint(false).build();
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
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(60).setStopWaitTime(0).setPointOrder((short)2).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(120).setStopWaitTime(60).setPointOrder((short)3).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(180).setStopWaitTime(0).setPointOrder((short)4).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(240).setStopWaitTime(0).setPointOrder((short)5).build());
		td.add(TimeDemandGroupPoint.newBuilder().setTotalDriveTime(300).setStopWaitTime(0).setPointOrder((short)6).build());
		return td.build();
	}

	
	public Journey getJourney(int journeypatternVersion){
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		Journey.Builder j = Journey.newBuilder()
			.setAgencyId("QBUZZ")
			.setDeparturetime(c.get(Calendar.HOUR_OF_DAY)*60*60+c.get(Calendar.MINUTE)*60+c.get(Calendar.SECOND))
			.setId("2552611")
			.setOperatingDay(df.format(c.getTime()))
			.setPrivateCode("QBUZZ:g005:1045")
			.setAvailabilityConditionRef(0L)
			.setTimeDemandGroup(testGroup());
		if (journeypatternVersion == 0){
			j.setJourneyPattern(testPattern());
		}else if (journeypatternVersion == 1){
			j.setJourneyPattern(testPattern2());
		}else if (journeypatternVersion == 2){
			j.setJourneyPattern(testPattern2());
		}
		return j.build();
	}
	@Test
	public void testNegativeOnFirstStopAsTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException{
		Journey journey = getJourney(0);
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
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo,true).getGtfsRealtimeTrip();
		assertEquals(1,tripUpdate.getStopTimeUpdateCount());
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),journey.getDepartureEpoch()-60);
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getDelay(),-60);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getTime(),journey.getDepartureEpoch());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getDelay(),0);

	}

	@Test
	public void testNegativeOnFirstStopNotAsTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException{
		Journey journey = getJourney(1);
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
		posinfo.setTimestamp(journey.getDepartureEpoch()-120);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo,true).getGtfsRealtimeTrip();
		assertTrue(tripUpdate.getStopTimeUpdateCount() == 1);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getTime(),journey.getDepartureEpoch()-120);
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getDelay(),-120);
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getTime(),journey.getDepartureEpoch());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getDelay(),0);

	}
	@Test
	public void testNegativeOnTimingPoint() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException{
		Journey journey = getJourney(2);
		JourneyProcessor j = new JourneyProcessor(journey);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday(journey.getOperatingDay());
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.ARRIVAL);
		posinfo.setUserstopcode(journey.getJourneypattern().getPoint((short)3).getOperatorpointref());
		posinfo.setPunctuality(-30);
		posinfo.setTimestamp(journey.getDepartureEpoch()+100);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo,true).getGtfsRealtimeTrip();
		assertEquals(2,tripUpdate.getStopTimeUpdateCount());
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getArrival().getDelay(),0);
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getTime(),journey.getDepartureEpoch());
		assertEquals(tripUpdate.getStopTimeUpdate(1).getArrival().getDelay(),-20);
		assertEquals(tripUpdate.getStopTimeUpdate(1).getDeparture().getDelay(),0);
	}

	@Test
	public void testNegativeOnDepartureFirstStop() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException{
		Journey journey = getJourney(0);
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
		TripUpdate.Builder tripUpdate = j.update(posinfo,true).getGtfsRealtimeTrip();
		assertEquals(1,tripUpdate.getStopTimeUpdateCount());
		assertFalse(tripUpdate.getStopTimeUpdate(0).hasArrival());
		assertEquals(tripUpdate.getStopTimeUpdate(0).getDeparture().getDelay(),0);
	}

	@Test
	public void testDelayOnDepartureFirstStop() throws StopNotFoundException, UnknownKV6PosinfoType, TooEarlyException, TooOldException, ParseException{
		Journey journey = getJourney(0);
		JourneyProcessor j = new JourneyProcessor(journey);
		KV6posinfo posinfo = new KV6posinfo();
		posinfo.setDataownercode(DataOwnerCode.QBUZZ);
		posinfo.setLineplanningnumber("g005");
		posinfo.setOperatingday(journey.getOperatingDay());
		posinfo.setJourneynumber(1034);
		posinfo.setVehiclenumber(911);
		posinfo.setMessagetype(Type.DEPARTURE);
		posinfo.setUserstopcode("10006900");
		posinfo.setPunctuality(+100);
		posinfo.setTimestamp(journey.getDepartureEpoch()+20);
		posinfo.setPassagesequencenumber(0);
		TripUpdate.Builder tripUpdate = j.update(posinfo).getGtfsRealtimeTrip();
		assertEquals(4,tripUpdate.getStopTimeUpdateCount());
		assertTrue(tripUpdate.getStopTimeUpdate(0).hasDeparture());
		assertEquals(56,tripUpdate.getStopTimeUpdate(3).getDeparture().getDelay());
	}
}
