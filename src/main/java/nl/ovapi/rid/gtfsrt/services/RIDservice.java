package nl.ovapi.rid.gtfsrt.services;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import nl.ovapi.bison.sax.DateUtils;
import nl.ovapi.rid.Database;
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
	private Map<String, ArrayList<Journey>> trains = Maps.newHashMapWithExpectedSize(0);
	private Map<String, TimeDemandGroup> timedemandgroups = Maps.newHashMapWithExpectedSize(0);
	private Map<String, JourneyPattern> journeypatterns = Maps.newHashMapWithExpectedSize(0);
	private Map<String, StopPoint> stoppoints = Maps.newHashMapWithExpectedSize(0);
	private Map<String, ArrayList<Long>> userstops = Maps.newHashMapWithExpectedSize(50000);
	private Map<String, ArrayList<String>> lines = Maps.newHashMapWithExpectedSize(500);
	private final static int HOUR_TO_RUN_UPDATE = 2;

	public RIDservice(){}

	public Journey getJourney(String id){
		String key = hf.hashString(id).toString();
		return journeys.get(key);
	}

	public ArrayList<Journey> getTrains(String id){
		String key = hf.hashString(id).toString();
		if (trains.containsKey(key)){
			return trains.get(key);
		}
		ArrayList<Journey> journey = new ArrayList<Journey>();
		journey.add(journeys.get(key));
		return journey;
	}

	public Map<String, Journey> getJourneys(){
		return journeys;
	}

	public void deleteJourney(String id){
		journeys.remove(id);
	}

	public ArrayList<String> getLineIds(DataOwnerCode daow, String linePlanningNumber){
		String id = String.format("%s:%s", daow.name(),linePlanningNumber);
		if (lines.containsKey(id)){
			return lines.get(id);
		}else{
			_log.info("Line {} not found",id);
			return null;
		}
	}


	public ArrayList<Long> getStopIds(DataOwnerCode daow, String userstopcode){
		String id = String.format("%s:%s", daow.name(),userstopcode);
		if (userstops.containsKey(id)){
			return userstops.get(id);
		}else{
			_log.info("Stop {} not found",id);
			return null;
		}
	}

	public StopPoint getStopPoint(Long id){
		if (stoppoints.containsKey(id)){
			return stoppoints.get(id);
		}
		return null;
	}

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
		_log.info("Done loading\n"+simpleStats());
		_scheduler = Executors.newScheduledThreadPool(5);

		Calendar c = Calendar.getInstance();
		long now = c.getTimeInMillis();
		c.setTimeZone(TimeZone.getTimeZone("Europe/Amsterdam"));		
		c.add(Calendar.DAY_OF_MONTH,1);
		c.set(Calendar.HOUR_OF_DAY, HOUR_TO_RUN_UPDATE);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		int toNextRun = (int)((c.getTimeInMillis() - now)/1000/60); // time to 2am
		int betweenRuns = 60*24; //24h in minutes
		_scheduler.scheduleWithFixedDelay(new UpdateTask(), toNextRun, betweenRuns, TimeUnit.MINUTES);
	}

	private class UpdateTask implements Runnable {
		@Override
		public void run() {
			update();
		}
	}

	public void update(){
		try {
			Connection conn = getConn();
			try{
				update(conn);
			}finally {
				conn.close();
			}
		} catch (SQLException e) {
			_log.error("Loading SQL crash", e);
		}
	}

	public void update(Connection  conn){
		try {
			PreparedStatement st = conn.prepareStatement(Database.timepatternQuery);
			ResultSet rs = st.executeQuery();
			String timedemandgroupref = null;
			TimeDemandGroup group = null;
			Map<String, TimeDemandGroup> newTimedemandgroups = Maps.newHashMapWithExpectedSize(10000);
			Map<String, JourneyPattern> newJourneypatterns = Maps.newHashMapWithExpectedSize(5000);
			Map<String, Journey> newJourneys = Maps.newHashMapWithExpectedSize(0);
			Map<String, ArrayList<Journey>> newTrains = Maps.newHashMapWithExpectedSize(0);
			while (rs.next()) {
				//timedemandgroupref,pointorder,totaldrivetime,stopwaittime
				String curRef = rs.getString(1).intern();
				if (!curRef.equals(timedemandgroupref)){
					timedemandgroupref = curRef;
					if (timedemandgroups.containsKey(timedemandgroupref)){ //Interning
						newTimedemandgroups.put(timedemandgroupref, timedemandgroups.get(timedemandgroupref));
						continue;
					}
					group = new TimeDemandGroup();
					newTimedemandgroups.put(timedemandgroupref, group);
				}
				TimeDemandGroupPoint point = new TimeDemandGroup.TimeDemandGroupPoint();
				point.setPointorder(rs.getInt(2));
				point.setTotaldrivetime(rs.getInt(3));
				point.setStopwaittime(rs.getInt(4));
				if (group != null)
					group.points.add(point);
			}
			timedemandgroups.put(timedemandgroupref, group);
			st = conn.prepareStatement(Database.journeyPatternQuery);
			rs = st.executeQuery();
			String journeypatternRef = null;
			JourneyPattern jp = null;
			while (rs.next()) {
				String curRef = rs.getString(1).intern();
				if (!curRef.equals(journeypatternRef)){
					journeypatternRef = curRef;
					if (journeypatterns.containsKey(journeypatternRef)){ //Recycle
						journeypatternRef = null;
						newJourneypatterns.put(journeypatternRef, journeypatterns.get(timedemandgroupref));
						continue;
					}
					jp = new JourneyPattern();
					newJourneypatterns.put(journeypatternRef, jp);
				}
				JourneyPatternPoint point = new JourneyPattern.JourneyPatternPoint();
				point.setPointorder(rs.getInt(2));
				point.setPointref(rs.getLong(3));
				point.setOperatorpointref(rs.getString(4));
				point.setIswaitpoint(rs.getBoolean(5));
				point.setDistancefromstartroute(rs.getInt(6));
				point.setIsscheduled(true);
				jp.points.add(point);
			}
			journeypatterns.put(journeypatternRef, jp);
			st = conn.prepareStatement(Database.journeyQuery);
			_log.info("Start query for journeys");
			rs = st.executeQuery();
			while (rs.next()) {
				String key = hf.hashString(rs.getString(1)).toString();
				if (journeys.containsKey(key)){
					newJourneys.put(key, journeys.get(key));
					continue;
				}
				Journey journey = new Journey();
				journey.setId(rs.getLong(2));
				journey.setJourneypattern(newJourneypatterns.get(rs.getString(3).intern()));
				journey.setTimedemandgroup(newTimedemandgroups.get(rs.getString(4).intern()));
				journey.setDeparturetime(rs.getInt(5));
				if (rs.getString(6) != null){
					journey.setWheelchairaccessible("true".equals(rs.getString(6)));
				}
				journey.setAgencyId(rs.getString(7));
				journey.setOperatingDay(rs.getString(8));
				if (newJourneys.containsKey(key)){ //Trains can have multiple journeys under same trainnumer
					if (rs.getString(1).contains("ARR")){ 
						continue;//But Arriva is just mucking around ;)
					}
					if (newTrains.containsKey(key)){
						newTrains.get(key).add(journey);
					}else{
						ArrayList<Journey> trains = new ArrayList<Journey>();
						trains.add(journey);
						newTrains.put(key, trains);
					}
				}else{
					newJourneys.put(key, journey);
				}
			}
			st = conn.prepareStatement(Database.stoppointQuery);
			rs = st.executeQuery();
			while (rs.next()) {
				StopPoint sp = new StopPoint();
				sp.setLatitude(rs.getFloat(2));
				sp.setLongitude(rs.getFloat(3));
				String stopId = rs.getString(4);
				if (userstops.containsKey(stopId)){
					if (!userstops.get(stopId).contains(rs.getLong(1)))
						userstops.get(stopId).add(rs.getLong(1));
				}else{
					ArrayList<Long> ids = new ArrayList<Long>();
					ids.add(rs.getLong(1));
					userstops.put(stopId, ids);
				}
				if (!stoppoints.containsKey(rs.getLong(1)))
					stoppoints.put((rs.getString(1).intern()), sp);
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
					lines.put(lineId, ids);
				}
			}
			journeypatterns = newJourneypatterns;
			timedemandgroups = newTimedemandgroups;
			journeys = newJourneys;
			trains = newTrains;
		}catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(e.getStackTrace());
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
		sb.append(String.format("%d journeypatterns\n", journeypatterns.size()));
		sb.append(String.format("%d timepatterns\n", timedemandgroups.size()));
		sb.append(String.format("%d stoppoints\n", stoppoints.size()));
		return sb.toString();
	}
}
