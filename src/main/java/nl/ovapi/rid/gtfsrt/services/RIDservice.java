package nl.ovapi.rid.gtfsrt.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
	private Map<String, String> gvbJourneys = Maps.newHashMapWithExpectedSize(0);
	private final static int HOUR_TO_RUN_UPDATE = 2;
	@Getter private long fromDate = 0;

	public RIDservice(){}

	/**
	 * @param id OperatingDay+':'+DataOwnerCode+':'+LinePlanningNumber+':'+JourneyNumber
	 * @return If id does not exist NULL, else a Journey object with said identifier. 
	 */
	public Journey getJourney(String id){
		String key = hf.hashString(id).toString();
		return journeys.get(key);
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
	 * @param oldId OperatingDay+':'+DataOwnerCode+':'+LinePlanningNumber+':'+JourneyNumber in legacy KV1 deliveries.
	 * @return OperatingDay+':'+DataOwnerCode+':'+LinePlanningNumber+':'+JourneyNumber of new KV1 system, if known otherwise NULL.
	 */
	public String getGVBdeltaId(String oldId){
		String key = hf.hashString(oldId).toString();
		return gvbJourneys.get(key);
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
	public Long getRailStation(String station){
		String id = String.format("IFF:%s:0", station);
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
		Calendar c = Calendar.getInstance();
		c.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));		
		long now = c.getTimeInMillis();
		c.add(Calendar.DAY_OF_MONTH,1);
		c.set(Calendar.HOUR_OF_DAY, HOUR_TO_RUN_UPDATE);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		int toNextRun = (int)((c.getTimeInMillis() - now)/1000/60); // time to 2am
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
					if (timedemandgroups.containsKey(timedemandgroupref)){ //Interning
						newTimedemandgroups.put(timedemandgroupref, timedemandgroups.get(timedemandgroupref));
						continue;
					}
					group = TimeDemandGroup.newBuilder();
				}
				timedemandgroupref = curRef;
				TimeDemandGroupPoint point = TimeDemandGroup.TimeDemandGroupPoint.newBuilder()
						.setPointOrder(rs.getInt("pointorder"))
						.setTotalDriveTime(rs.getInt("totaldrivetime"))
						.setStopWaitTime(rs.getInt("stopwaittime")).build();
				group.add(point);
			}
			newTimedemandgroups.put(timedemandgroupref, group.build());
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
					if (journeypatterns.containsKey(journeypatternRef)){ //Recycle
						newJourneypatterns.put(journeypatternRef, journeypatterns.get(journeypatternRef));
						continue;
					}
					jp = JourneyPattern.newBuilder();
				}
				journeypatternRef = curRef;
				JourneyPatternPoint point = JourneyPatternPoint.newBuilder()
						.setPointOrder(rs.getInt("pointorder"))
						.setPointRef(rs.getLong("pointref"))
						.setOperatorPointRef(rs.getString("operatorpointref"))
						.setIsWaitpoint(rs.getBoolean("iswaitpoint"))
						.setDistanceFromStartRoute(rs.getInt("distancefromstartroute"))
						.setIsScheduled(rs.getBoolean("isscheduled"))
						.setDestinationCode(rs.getString("destinationcode"))
						.setPlatformCode(rs.getString("destinationcode"))
						.build();
				jp.add(point);
				jp.setDirectionType(rs.getInt("directiontype"));
				jp.setJourneyPatternref(journeypatternRef);
			}
			newJourneypatterns.put(journeypatternRef, jp.build());
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
				Journey journey = new Journey();
				journey.setId(rs.getLong(2)+"");
				journey.setJourneypattern(newJourneypatterns.get(rs.getString(3).intern()));
				if (journey.getJourneypattern() == null)
					_log.error("JourneyPattern == null {} {}",rs.getString(1),rs.getString(3));
				journey.setTimedemandgroup(newTimedemandgroups.get(rs.getString(4).intern()));
				if (journey.getTimedemandgroup() == null)
					_log.error("TimeDemandGroup == null {} {}",rs.getString(1),rs.getString(4));
				journey.setDeparturetime(rs.getInt(5));
				if (rs.getString(6) == null){
					journey.setWheelchairaccessible(null);
				}else{
					journey.setWheelchairaccessible(rs.getBoolean(6));
				}
				journey.setAgencyId(rs.getString(7));
				journey.setOperatingDay(rs.getString(8));
				if (journey.getEndEpoch() < (System.currentTimeMillis()/1000 - 3600*2)){
					continue;
				}
				long date = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString(8)).getTime();
				if (fromDate == 0 || date < fromDate){
					fromDate = date;
				}
				journey.setPrivateCode(rs.getString(9));
				journey.setRouteId(rs.getLong(10));
				journey.setAvailabilityConditionRef(rs.getLong(11));
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
				Journey journey = new Journey();
				journey.setId(rs.getLong(2)+"");
				journey.setJourneypattern(newJourneypatterns.get(rs.getString(3).intern()));
				if (journey.getJourneypattern() == null)
					_log.error("JourneyPattern == null {} {}",rs.getString(1),rs.getString(3));
				journey.setTimedemandgroup(newTimedemandgroups.get(rs.getString(4).intern()));
				if (journey.getTimedemandgroup() == null)
					_log.error("TimeDemandGroup == null {} {}",rs.getString(1),rs.getString(4));
				journey.setDeparturetime(rs.getInt(5));
				if (rs.getString(6) == null){
					journey.setWheelchairaccessible(null);
				}else{
					journey.setWheelchairaccessible(rs.getBoolean(6));
				}
				journey.setAgencyId(rs.getString(7));
				journey.setOperatingDay(rs.getString(8));
				long date = new SimpleDateFormat("yyyy-MM-dd").parse(rs.getString(8)).getTime();
				if (fromDate == 0 || date < fromDate){
					fromDate = date;
				}
				journey.setPrivateCode(rs.getString(9));
				journey.setRouteId(rs.getLong(10));
				journey.setBlockRef(rs.getString(11));
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
						.setLongitude(rs.getFloat("longitude")).Build();
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
			//TODO remove when GVB get their shit in order
			Map<String, String> newgvbJourneys = Maps.newHashMapWithExpectedSize(30000);
			st = conn.prepareStatement(Database.gvbJourneyQuery);
			rs = st.executeQuery();
			while (rs.next()) {
				String key = hf.hashString(rs.getString(1)).toString();
				String deltaId = rs.getString(2);
				newgvbJourneys.put(key, deltaId);
			}
			journeypatterns = newJourneypatterns;
			timedemandgroups = newTimedemandgroups;
			journeys = newJourneys;
			gvbJourneys = newgvbJourneys;
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
}
