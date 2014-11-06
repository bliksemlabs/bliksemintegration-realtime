package nl.ovapi.rid.gtfsrt.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import lombok.Getter;
import nl.ovapi.bison.DateUtils;
import nl.ovapi.bison.model.AdviceType;
import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.EffectType;
import nl.ovapi.bison.model.KV15message;
import nl.ovapi.bison.model.MessageDurationType;
import nl.ovapi.bison.model.MessagePriority;
import nl.ovapi.bison.model.MessageType;
import nl.ovapi.bison.model.ReasonType;
import nl.ovapi.bison.model.SubAdviceType;
import nl.ovapi.bison.model.SubEffectType;
import nl.ovapi.bison.model.SubMeasureType;
import nl.ovapi.bison.model.SubReasonType;
import nl.ovapi.rid.Database;
import nl.ovapi.rid.model.Block;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.JourneyPattern;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.StopPoint;
import nl.ovapi.rid.model.TimeDemandGroup;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

@Singleton
public class RIDservice {
	private static final Logger _log = LoggerFactory.getLogger(RIDservice.class);

	private ScheduledExecutorService _scheduler;
	private static final String url = "jdbc:postgresql://localhost/ridprod";
	private static final String user = "rid";
	private static final String password = "bliksem";
	private HashFunction hf = Hashing.crc32();

	private Map<String, Journey> journeys = Maps.newHashMapWithExpectedSize(0);
	private Map<String, ArrayList<Block>> trains = Maps.newHashMapWithExpectedSize(0);
	private Map<String, TimeDemandGroup> timedemandgroups = Maps.newHashMapWithExpectedSize(0);
	private Map<String, JourneyPattern> journeypatterns = Maps.newHashMapWithExpectedSize(0);
	private Map<String, StopPoint> stoppoints = Maps.newHashMapWithExpectedSize(0);
	private Map<String, ArrayList<Long>> userstops = Maps.newHashMapWithExpectedSize(50000);
	private Map<String, ArrayList<String>> lines = Maps.newHashMapWithExpectedSize(500);
	private final static int SECONDS_IN_A_DAY = 60 * 60 * 24;

	private final static int HOUR_TO_RUN_UPDATE = 2;
	@Getter private long fromDate = 0;
	@Getter private DateTimeZone timeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("Europe/Amsterdam"));
		DateTimeZone.setDefault(DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/Amsterdam")));
	}

	public RIDservice(){}

	/**
	 * @param id OperatingDay+':'+DataOwnerCode+':'+LinePlanningNumber+':'+JourneyNumber
	 * @return If id does not exist NULL, else a Journey object with said identifier. 
	 */
	public Journey getJourney(String id){
		String key = hf.hashString(id).toString();
		return journeys.get(key);
	}

	public Collection<Journey> getAllJourneys(){
		return journeys.values();
	}

	/**
	 * @param id OperatingDay+':'+DataOwnerCode+':'+LinePlanningNumber+':'+JourneyNumber
	 * @return If id does not exist NULL, else a List of blocks with said identifier. 
	 */
	public ArrayList<Block> getTrains(String id){
		String key = hf.hashString(id).toString();
		ArrayList<Block> blocks = trains.get(key);
		return blocks;
	}

	/**
	 * @param daow DataOwnerCode (eg. GVB , RET, HTM,etc)
	 * @param linePlanningNumber (eg. M300, 4045,etc.)
	 * @return NULL when DataOwnerCode and LinePlanningNumber does not exist within database.
	 *         List of id's of Lines with DataOwnerCode daow and UserStopCode userstopcode
	 */
	public ArrayList<String> getLineIds(DataOwnerCode daow, String linePlanningNumber){
		String id = String.format("%s:%s", daow.name(),linePlanningNumber);
		if (lines.containsKey(id)){
			return lines.get(id);
		}else{
			_log.info("Line {} not found",id);
			return null;
		}
	}

	/**
	 * @param daow DataOwnerCode (eg. GVB , RET, HTM,etc).
	 * @param userstopcode UserStopCode (eg. 49005010)
	 * @return NULL when DataOwnerCode daow and UserStopCode userstopcode does not exist in database.
	 *         List of id's of StopPoints with DataOwnerCode daow and UserStopCode userstopcode
	 */

	public ArrayList<Long> getStopIds(DataOwnerCode daow, String userstopcode){
		String id = String.format("%s:%s", daow.name(),userstopcode);
		if (userstops.containsKey(id)){
			return userstops.get(id);
		}else{
			_log.info("Stop {} not found",id);
			return null;
		}
	}

	/**
	 * 
	 * @param station NS stationcode 
	 * @return Long of identifier of stoppoint with undefined platform for that station
	 */
	public Long getRailStation(String station, String platformCode){
		if (platformCode == null){
			platformCode = "0";
		}
		String id = String.format("IFF:%s:%s", station,platformCode);
		ArrayList<Long> results = userstops.get(id);
		if (results == null || results.size() == 0){
			return null;
		}
		return results.get(0);
	}


	/**
	 * @param id of stoppoint in database.
	 * @return NULL when StopPoint with id is not present in database
	 *         StopPoint class with data present in database.
	 */

	public StopPoint getStopPoint(Long id){
		return stoppoints.get(id.toString().intern());
	}

	/**
	 * @return Active KV15messages stored in RID database
	 * @throws SQLException
	 */

	public ArrayList<KV15message> getActiveKV15messages() throws SQLException{
		ArrayList<KV15message> messages = new ArrayList<KV15message>();
		Connection conn = getConn();
		try{
			PreparedStatement st = conn.prepareStatement(Database.kv15Query);
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				KV15message message = new KV15message();
				message.setIsDelete(false);
				message.setDataOwnerCode(DataOwnerCode.valueOf(rs.getString(1)));
				message.setMessageCodeDate(rs.getString(2));
				message.setMessageCodeNumber(rs.getInt(3));
				if (rs.getString(4) != null){
					for (String userstopcode : rs.getString(4).split(";")){
						message.addUserstopCode(userstopcode);
					}
				}
				message.setMessagePriority(MessagePriority.valueOf(rs.getString(5)));
				message.setMessageType(MessageType.valueOf(rs.getString(6)));
				message.setMessageDurationType(MessageDurationType.valueOf(rs.getString(7)));
				message.setMessageStartTime(DateUtils.parse(rs.getString(8)));
				message.setMessageEndTime(DateUtils.parse(rs.getString(9)));
				message.setMessageContent(rs.getString(10));
				message.setReasonType(ReasonType.parse(rs.getString(11)));
				message.setSubReasonType(SubReasonType.parse(rs.getString(12)));
				message.setReasonContent(rs.getString(13));
				message.setEffectType(EffectType.parse(rs.getString(14)));
				message.setSubEffectType(SubEffectType.parse(rs.getString(15)));
				message.setEffectContent(rs.getString(16));
				message.setAdviceType(AdviceType.parse(rs.getString(17)));
				message.setSubAdviceType(SubAdviceType.parse(rs.getString(18)));
				message.setAdviceContent(rs.getString(19));
				message.setMessageTimeStamp(DateUtils.parse(rs.getString(20)));
				if (rs.getString(21) != null)
					message.setMeasureType(Integer.parseInt(rs.getString(21)));
				message.setSubMeasureType(SubMeasureType.parse(rs.getString(22)));
				message.setMeasureContent(rs.getString(23));
				if (rs.getString(24) != null){
					for (String linePlanningNumber : rs.getString(24).split(";")){
						message.addLinePlanningNumber(linePlanningNumber);
					}
				}
				messages.add(message);
			}
		}finally{
			conn.close();
		}
		return messages;
	}

	@PostConstruct
	public void start() throws SQLException {
		update();
		_scheduler = Executors.newScheduledThreadPool(5);

		DateTime dt = DateTime.now();
		long now = dt.getMillis();
		dt = dt.plusDays(1).withTime(HOUR_TO_RUN_UPDATE,0,0,0);
		int toNextRun = (int)((dt.getMillis() - now)/1000/60); // time to 2am
		int betweenRuns = 24*60; //24h in minutes
		_scheduler.scheduleWithFixedDelay(new UpdateTask(), toNextRun, betweenRuns, TimeUnit.MINUTES);
	}

	private class UpdateTask implements Runnable {
		@Override
		public void run() {
			update();
		}
	}

	private void update(){
		try {
			Connection conn = getConn();
			try{
				update(conn);
			}finally {
				conn.close();
			}
			_log.info("Done loading\n"+simpleStats());
		} catch (SQLException e) {
			_log.error("Loading SQL crash", e);
		}
	}

	private void update(Connection  conn){
		try {
			fromDate = 0;
			PreparedStatement st = conn.prepareStatement(Database.timepatternQuery);
			ResultSet rs = st.executeQuery();
			String timedemandgroupref = null;
			TimeDemandGroup.Builder group = null;
			Map<String, TimeDemandGroup> newTimedemandgroups = Maps.newHashMapWithExpectedSize(10000);
			Map<String, JourneyPattern> newJourneypatterns = Maps.newHashMapWithExpectedSize(5000);
			Map<String, Journey> newJourneys = Maps.newHashMapWithExpectedSize(0);
			Map<String, ArrayList<Block>> newTrains = Maps.newHashMapWithExpectedSize(0);
			while (rs.next()) {
				//timedemandgroupref,pointorder,totaldrivetime,stopwaittime
				String curRef = rs.getString(1).intern();
				if (!curRef.equals(timedemandgroupref)){
					if (group != null){
						newTimedemandgroups.put(timedemandgroupref, group.build());
					}
					group = null;
					if (timedemandgroups.containsKey(curRef)){ //Interning
						newTimedemandgroups.put(curRef, timedemandgroups.get(curRef));
						continue;
					}
					group = TimeDemandGroup.newBuilder();
				}
				timedemandgroupref = curRef;
				TimeDemandGroupPoint point = TimeDemandGroup.TimeDemandGroupPoint.newBuilder()
						.setPointOrder(rs.getShort("pointorder"))
						.setTotalDriveTime(rs.getInt("totaldrivetime"))
						.setStopWaitTime(rs.getInt("stopwaittime")).build();
				group.add(point);
			}
			if (group != null){
				newTimedemandgroups.put(timedemandgroupref, group.build());
			}
			st = conn.prepareStatement(Database.journeyPatternQuery);
			rs = st.executeQuery();
			String journeypatternRef = null;
			JourneyPattern.Builder jp = null;
			while (rs.next()) {
				String curRef = rs.getString(1).intern();
				if (!curRef.equals(journeypatternRef)){
					if (jp != null){
						newJourneypatterns.put(journeypatternRef, jp.build());
					}
					jp = null;
					if (journeypatterns.containsKey(curRef)){ //Recycle
						newJourneypatterns.put(curRef, journeypatterns.get(curRef));
						continue;
					}
					jp = JourneyPattern.newBuilder();
				}
				journeypatternRef = curRef;
				JourneyPatternPoint point = JourneyPatternPoint.newBuilder()
						.setPointOrder(rs.getShort("pointorder"))
						.setPointRef(rs.getLong("pointref"))
						.setOperatorPointRef(rs.getString("operatorpointref"))
						.setIsWaitpoint(rs.getBoolean("iswaitpoint"))
						.setDistanceFromStartRoute(rs.getInt("distancefromstartroute"))
						.setIsScheduled(rs.getBoolean("isscheduled"))
						.setDestinationCode(rs.getString("destinationcode"))
						.setPlatformCode(rs.getString("platformcode"))
						.setForBoarding(rs.getBoolean("forboarding"))
						.setForAlighting(rs.getBoolean("foralighting"))
						.build();
				jp.add(point);
				jp.setDirectionType(rs.getByte("directiontype"));
				jp.setJourneyPatternref(journeypatternRef);
			}
			if (jp != null){
				newJourneypatterns.put(journeypatternRef, jp.build());
			}
			st = conn.prepareStatement(Database.journeyQuery);
			rs = st.executeQuery();
			int newCount = 0;
			while (rs.next()) {
				String key = hf.hashString(rs.getString(1)).toString();
				long id = rs.getLong(2);
				if (journeys.containsKey(key) && journeys.get(key).getId().equals(id)){
					newJourneys.put(key, journeys.get(key));
					continue;
				}
				newCount++;
				Journey journey = Journey.newBuilder()
						.setId(rs.getString("id"))
						.setJourneyPattern(newJourneypatterns.get(rs.getString("journeypatternref")))
						.setTimeDemandGroup(newTimedemandgroups.get(rs.getString("timedemandgroupref")))
						.setAgencyId(rs.getString("operatorcode"))
						.setDeparturetime(rs.getInt("departuretime"))
						.setOperatingDay(rs.getString("validdate"))
						.setPrivateCode(rs.getString("privatecode"))
						.setRouteId(rs.getLong("lineref"))
						.setAvailabilityConditionRef(rs.getLong("availabilityconditionref"))
						.setWheelchairaccessible(rs.getString("wheelchairaccessible") == null ? 
								null : rs.getBoolean("wheelchairaccessible")).build();
				long date = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString(8)).getTime();
				if (fromDate == 0 || date < fromDate){
					fromDate = date;
				}				
				if (journey.getJourneypattern() == null)
					_log.error("JourneyPattern == null {} {}",rs.getString(1),rs.getString(3));
				if (journey.getTimedemandgroup() == null)
					_log.error("TimeDemandGroup == null {} {}",rs.getString(1),rs.getString(4));
				if (newJourneys.containsKey(key)){ //Trains can have multiple journeys under same trainnumer
					_log.info("Duplicate privatecodes ignoring one of {}",rs.getString(1));
				}else{
					newJourneys.put(key, journey);
				}
			}
			_log.info("{} New journeys",newCount);

			st = conn.prepareStatement(Database.trainQuery);
			rs = st.executeQuery();
			Block block = null;
			while (rs.next()) {
				String key = hf.hashString(rs.getString(1)).toString();
				Journey journey = Journey.newBuilder()
						.setId(rs.getString("id"))
						.setJourneyPattern(newJourneypatterns.get(rs.getString("journeypatternref")))
						.setTimeDemandGroup(newTimedemandgroups.get(rs.getString("timedemandgroupref")))
						.setAgencyId(rs.getString("operatorcode"))
						.setDeparturetime(rs.getInt("departuretime"))
						.setOperatingDay(rs.getString("validdate"))
						.setPrivateCode(rs.getString("privatecode"))
						.setRouteId(rs.getLong("lineref"))
						.setBlockRef(rs.getString("blockref"))
						.setWheelchairaccessible(rs.getString("wheelchairaccessible") == null ? 
								null : rs.getBoolean("wheelchairaccessible")).build();
				long date = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString(8)).getTime();
				if (fromDate == 0 || date < fromDate){
					fromDate = date;
				}
				if (journey.getJourneypattern() == null)
					_log.error("JourneyPattern == null {} {}",rs.getString(1),rs.getString(3));
				if (journey.getTimedemandgroup() == null)
					_log.error("TimeDemandGroup == null {} {}",rs.getString(1),rs.getString(4));
				if (block != null && block.getBlockRef().equals(journey.getBlockRef())){
					block.addJourney(journey);
				}else{
					block = new Block(journey.getBlockRef());
					block.addJourney(journey);
					ArrayList<Block> blocks = newTrains.get(key);
					if (blocks == null){
						blocks = new ArrayList<Block>();
					}
					blocks.add(block);
					newTrains.put(key, blocks);
				}
			}			
			st = conn.prepareStatement(Database.stoppointQuery);
			rs = st.executeQuery();
			while (rs.next()) {
				StopPoint sp = StopPoint.newBuilder()
						.setLatitude(rs.getFloat("latitude"))
						.setLongitude(rs.getFloat("longitude")).build();
				String stopId = rs.getString(4);
				if (userstops.containsKey(stopId)){
					if (!userstops.get(stopId).contains(rs.getLong(1)))
						userstops.get(stopId).add(rs.getLong(1));
				}else{
					ArrayList<Long> ids = new ArrayList<Long>();
					ids.add(rs.getLong(1));
					userstops.put(stopId, ids);
				}
				stoppoints.put(rs.getString(1).intern(), sp);
			}
			st = conn.prepareStatement(Database.lineQuery);
			rs = st.executeQuery();
			while (rs.next()) {
				String lineOperatorId = rs.getString(2);
				String lineId = rs.getString(1);
				if (lines.containsKey(lineOperatorId)){
					if (!lines.get(lineOperatorId).contains(rs.getLong(1)))
						lines.get(lineOperatorId).add(lineId);
				}else{
					ArrayList<String> ids = new ArrayList<String>();
					ids.add(lineId);
					lines.put(lineOperatorId, ids);
				}
			}
			journeypatterns = newJourneypatterns;
			timedemandgroups = newTimedemandgroups;
			journeys = newJourneys;
			trains = newTrains;
		}catch (Exception e) {
			_log.error("Loading SQL crash", e);
			e.printStackTrace();
		}
	}

	@PreDestroy
	public void stop() {
		if (_scheduler != null) {
			_scheduler.shutdownNow();
			_scheduler = null;
		}
	}

	private Connection getConn() throws SQLException{
		return DriverManager.getConnection(url, user, password);
	}

	private String simpleStats(){
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%d journeys\n", journeys.size()));
		sb.append(String.format("%d trains\n", trains.size()));
		sb.append(String.format("%d journeypatterns\n", journeypatterns.size()));
		sb.append(String.format("%d timepatterns\n", timedemandgroups.size()));
		sb.append(String.format("%d stoppoints\n", stoppoints.size()));
		return sb.toString();
	}

	/**
	 * Check if a trip with departureTime in TimeZone tz on date, is in the DST gap (between 2 and 3 when switchting to DST)
	 * These trips will never occur
	 * @param date serviceDate
	 * @param tz Timezone of the region
	 * @param departureTime departureTime for trip in seconds from midnight.
	 * @return whether the trip is in the DST gap.
	 */
	public static boolean tripDepartureInDSTGap(LocalDate date, DateTimeZone tz, int departureTime) {
		int seconds = departureTime;
		int days = seconds / SECONDS_IN_A_DAY;
		seconds = seconds % SECONDS_IN_A_DAY;
		int hourOfDay = seconds / 3600;
		seconds = seconds % 3600;
		int minutes = seconds / 60;
		seconds = seconds % 60;

		LocalDateTime ldt = new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), hourOfDay, minutes, seconds);
		if (days == 0) {
			if (tz.isLocalDateTimeGap(ldt)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return a DateTime for a trip with departureTime and time since start since departuretime.
	 * Both are in seconds since midnight
	 * @param date serviceday of the trip
	 * @param tz Timezone of the region
	 * @param departureTime departureTime of the trip in seconds from midnight
	 * @param driveTime time since start of trip in seconds
	 * @return DateTime object with the time of departuretime+drivetime with the correct epoch.
	 */
	public static DateTime toDateTime(LocalDate date, DateTimeZone tz, int departureTime, int driveTime) {
		int seconds = departureTime;
		int days = seconds / SECONDS_IN_A_DAY;
		seconds = seconds % SECONDS_IN_A_DAY;
		int hourOfDay = seconds / 3600;
		seconds = seconds % 3600;
		int minutes = seconds / 60;
		seconds = seconds % 60;

		LocalDateTime ldt = new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), hourOfDay, minutes, seconds);
		if (days == 0) {
			if (tz.isLocalDateTimeGap(ldt)) {
				return null; //Time does not exist
			}
			DateTime dt = new DateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), hourOfDay, minutes, seconds);
			return dt.plusSeconds(driveTime);
		}
		long epoch = date.toDateTime(new LocalTime(4, 0, 0), tz).getMillis() - TimeUnit.MILLISECONDS.convert(4, TimeUnit.HOURS);
		epoch += TimeUnit.MILLISECONDS.convert(days, TimeUnit.DAYS) +
				TimeUnit.MILLISECONDS.convert(hourOfDay, TimeUnit.HOURS) +
				TimeUnit.MILLISECONDS.convert(minutes, TimeUnit.MINUTES) +
				TimeUnit.MILLISECONDS.convert(seconds, TimeUnit.SECONDS) + driveTime * 1000;
		return new DateTime(epoch).toDateTime(tz);
	}

	/**
	 * Check if a trip with departureTime on date, is in the DST gap (between 2 and 3 when switching to DST)
	 * These trips will never occur
	 * @param date serviceDate
	 * @param departureTime departureTime for trip in seconds from midnight.
	 * @return whether the trip is in the DST gap.
	 */
	public boolean tripDepartureInDSTGap(LocalDate date, int departureTime) {
		return tripDepartureInDSTGap(date, getTimeZone(), departureTime);
	}

	/**
	 * Return a DateTime for a trip with departureTime and time since start since departuretime.
	 * Both are in seconds since midnight
	 * @param date serviceday of the trip
	 * @param departureTime departureTime of the trip in seconds from midnight
	 * @param driveTime time since start of trip in seconds
	 * @return DateTime object with the time of departuretime+drivetime with the correct epoch.
	 */
	public DateTime toDateTime(LocalDate date, int departureTime, int driveTime) {
		return toDateTime(date, getTimeZone(), departureTime, driveTime);
	}
}
