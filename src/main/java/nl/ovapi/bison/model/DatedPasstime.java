package nl.ovapi.bison.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.ovapi.bison.DateUtils;
import nl.ovapi.rid.gtfsrt.Utils;

import com.google.common.base.Objects;

@ToString()
/**
 * DatedPasstime Koppelvlak 8 record.
 * mofications to this class set the LastUpdateTimeStamp field to CurrentTime
 * @author Thomas Koch
 *
 */
public class DatedPasstime {

	@Getter private DataOwnerCode dataOwnerCode;
	@Getter private String operationDate;
	@Getter private String linePlanningNumber;
	@Getter private Integer journeyNumber;
	@Getter private Integer fortifyOrderNumber;
	@Getter private Integer userStopOrderNumber;
	@Getter private String userStopCode;
	@Getter private Integer localServiceLevelCode;
	@Getter private Integer journeyPatternCode;
	@Getter private Integer lineDirection;
	@Getter private Long lastUpdateTimeStamp;
	@Getter private String destinationCode;
	@Getter private boolean isTimingStop;
	@Getter private Integer expectedArrivalTime;
	@Getter private Integer expectedDepartureTime;
	@Getter private TripStopStatus tripStopStatus;
	@Getter private String messageContent;
	@Getter private MessageType messageType;
	@Getter private String sideCode;
	@Getter private Integer numberOfCoaches;
	@Getter private WheelChairAccessible wheelChairAccessible;
	@Getter private String operatorCode;
	@Getter private String reasonType;
	@Getter private String subReasonType;
	@Getter private String reasonContent;
	@Getter private String adviceType;
	@Getter private String subAdviceType;
	@Getter private String adviceContent;
	@Getter private DataOwnerCode timingPointDataOwnerCode;
	@Getter private String timingPointCode;
	@Getter private JourneyStopType journeyStopType;
	@Getter private Integer targetArrivalTime;
	@Getter private Integer targetDepartureTime;
	@Getter private Integer recordedArrivalTime;
	@Getter private Integer recordedDepartureTime;
	@Getter private boolean forBoarding;
	@Getter private boolean forAlighting;

	/**
	 * KV17 lag in seconds
	 */
	@Getter @Setter private Integer lag;

	/**
	 * Distance since start trip in meters
	 */
	@Getter @Setter private Integer distanceDriven;

	public void setForBoarding(boolean forBoarding) {
		if (!Objects.equal(forBoarding, this.forBoarding)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.forBoarding = forBoarding;
	}

	public void setForAlighting(boolean forAlighting) {
		if (!Objects.equal(forAlighting, this.forAlighting)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.forAlighting = forAlighting;
	}

	public void setReasonType(String reasonType) {
		if (!Objects.equal(reasonType, this.reasonType)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.reasonType = reasonType;
	}

	public void setOperatorCode(String operatorCode) {
		if (!Objects.equal(operatorCode, this.operatorCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.operatorCode = operatorCode;
	}

	public void setWheelChairAccessible(WheelChairAccessible wheelChairAccessible) {
		if (!Objects.equal(wheelChairAccessible, this.wheelChairAccessible)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.wheelChairAccessible = wheelChairAccessible;
	}

	public void setNumberOfCoaches(Integer numberOfCoaches) {
		if (!Objects.equal(numberOfCoaches, this.numberOfCoaches)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.numberOfCoaches = numberOfCoaches;
	}

	public void setSideCode(String sideCode) {
		if (!Objects.equal(sideCode, this.sideCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.sideCode = sideCode;
	}

	public void setMessageType(MessageType messageType) {
		if (!Objects.equal(messageType, this.messageType)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.messageType = messageType;
	}

	public void setMessageContent(String messageContent) {
		if (!Objects.equal(messageContent, this.messageContent)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.messageContent = messageContent;
	}

	public void setTripStopStatus(TripStopStatus tripStopStatus) {
		if (!Objects.equal(tripStopStatus, this.tripStopStatus)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.tripStopStatus = tripStopStatus;
	}

	public void setExpectedDepartureTime(Integer expectedDepartureTime) {
		if (!Objects.equal(expectedDepartureTime, this.expectedDepartureTime)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.expectedDepartureTime = expectedDepartureTime;
	}

	public void setExpectedArrivalTime(Integer expectedArrivalTime) {
		if (!Objects.equal(expectedArrivalTime, this.expectedArrivalTime)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.expectedArrivalTime = expectedArrivalTime;
	}

	public void setTimingStop(boolean isTimingStop) {
		if (!Objects.equal(isTimingStop, this.isTimingStop)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.isTimingStop = isTimingStop;
	}

	public void setDestinationCode(String destinationCode) {
		if (!Objects.equal(destinationCode, this.destinationCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.destinationCode = destinationCode;
	}

	public void setLastUpdateTimeStamp(Long lastUpdateTimeStamp) {
		this.lastUpdateTimeStamp = lastUpdateTimeStamp;
	}

	public void setLineDirection(Integer lineDirection) {
		if (!Objects.equal(lineDirection, this.lineDirection)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.lineDirection = lineDirection;
	}

	public void setJourneyPatternCode(Integer journeyPatternCode) {
		if (!Objects.equal(journeyPatternCode, this.journeyPatternCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.journeyPatternCode = journeyPatternCode;
	}

	public void setLocalServiceLevelCode(Integer localServiceLevelCode) {
		if (!Objects.equal(localServiceLevelCode, this.localServiceLevelCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.localServiceLevelCode = localServiceLevelCode;
	}

	public void setUserStopCode(String userStopCode) {
		if (!Objects.equal(userStopCode, this.userStopCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.userStopCode = userStopCode;
	}

	public void setUserStopOrderNumber(Integer userStopOrderNumber) {
		if (!Objects.equal(userStopOrderNumber, this.userStopOrderNumber)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.userStopOrderNumber = userStopOrderNumber;
	}

	public void setFortifyOrderNumber(Integer fortifyOrderNumber) {
		if (!Objects.equal(fortifyOrderNumber, this.fortifyOrderNumber)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.fortifyOrderNumber = fortifyOrderNumber;
	}

	public void setJourneyNumber(Integer journeyNumber) {
		if (!Objects.equal(journeyNumber, this.journeyNumber)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.journeyNumber = journeyNumber;
	}

	public void setLinePlanningNumber(String linePlanningNumber) {
		if (!Objects.equal(linePlanningNumber, this.linePlanningNumber)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.linePlanningNumber = linePlanningNumber;
	}

	public void setOperationDate(String operationDate) {
		if (!Objects.equal(operationDate, this.operationDate)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.operationDate = operationDate;
	}

	public void setDataOwnerCode(DataOwnerCode dataOwnerCode) {
		if (!Objects.equal(dataOwnerCode, this.dataOwnerCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.dataOwnerCode = dataOwnerCode;
	}

	public void setSubReasonType(String subReasonType) {
		if (!Objects.equal(subReasonType, this.subReasonType)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.subReasonType = subReasonType;
	}

	public void setReasonContent(String reasonContent){
		if (!Objects.equal(reasonContent, this.reasonContent)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.reasonContent = reasonContent;
	}

	public void setAdviceType(String adviceType){
		if (!Objects.equal(adviceType, this.adviceType)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.adviceType = adviceType;
	}	

	public void setSubAdviceType(String subAdviceType){
		if (!Objects.equal(subAdviceType, this.subAdviceType)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.subAdviceType = subAdviceType;
	}	

	public void setAdviceContent(String adviceContent){
		if (!Objects.equal(adviceContent, this.adviceContent)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.adviceContent = adviceContent;
	}	

	public void setTimingPointDataOwnerCode(DataOwnerCode timingPointDataOwnerCode){
		if (!Objects.equal(timingPointDataOwnerCode, this.timingPointDataOwnerCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.timingPointDataOwnerCode = timingPointDataOwnerCode;
	}

	public void setTimingPointCode(String timingPointCode){
		if (!Objects.equal(timingPointCode, this.timingPointCode)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.timingPointCode = timingPointCode;
	}

	public void setJourneyStopType(JourneyStopType journeyStopType){
		if (!Objects.equal(journeyStopType, this.journeyStopType)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.journeyStopType = journeyStopType;
	}

	public void setTargetDepartureTime(Integer targetDepartureTime){
		if (!Objects.equal(targetDepartureTime, this.targetDepartureTime)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.targetDepartureTime = targetDepartureTime;
	}

	public void setTargetArrivalTime(Integer targetArrivalTime){
		if (!Objects.equal(targetArrivalTime, this.targetArrivalTime)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.targetArrivalTime = targetArrivalTime;
	}

	public void setRecordedDepartureTime(Integer recordedDepartureTime){
		if (!Objects.equal(recordedDepartureTime, this.recordedDepartureTime)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.recordedDepartureTime = recordedDepartureTime;
	}

	public void setRecordedArrivalTime(Integer recordedArrivalTime){
		if (!Objects.equal(recordedArrivalTime, this.recordedArrivalTime)){
			this.setLastUpdateTimeStamp(Utils.currentTimeSecs());
		}
		this.recordedArrivalTime = recordedArrivalTime;
	}

	private static int secondsSinceMidnight(String hhmmss) {
		String[] time = hhmmss.split(":");
		int hours = Integer.parseInt(time[0]);
		int minutes = Integer.parseInt(time[1]);
		int seconds = Integer.parseInt(time[2]);
		return (hours * 60 + minutes) * 60 + seconds;
	}

	private static String to32Time(Integer secondsSinceMidnight) {
		if (secondsSinceMidnight == null){
			return null;
		}
		int hours = secondsSinceMidnight/3600;
		int seconds = secondsSinceMidnight % 3600;
		int minutes = seconds / 60;
		seconds = seconds % 60;
		return String.format("%02d:%02d:%02d",hours,minutes,seconds);
	}


	public String toCtxLine(){
		StringBuilder sb = new StringBuilder();
		sb.append(dataOwnerCode.name()).append('|');
		sb.append(operationDate).append('|');
		sb.append(linePlanningNumber).append('|');
		sb.append(journeyNumber).append('|');
		sb.append(fortifyOrderNumber).append('|');
		sb.append(userStopOrderNumber).append('|');
		sb.append(userStopCode).append('|');
		sb.append(localServiceLevelCode).append('|');
		sb.append(journeyPatternCode).append('|');
		sb.append(lineDirection).append('|');
		SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		sb.append(iso8601.format(new Date(lastUpdateTimeStamp*1000))).append('|');
		sb.append(destinationCode).append('|');
		sb.append(isTimingStop ? 1 : 0).append('|');
		String eta = to32Time(expectedArrivalTime);
		sb.append(eta == null ? "\\0" : eta).append('|');
		String etd = to32Time(expectedArrivalTime);
		sb.append(etd == null ? "\\0" : etd).append('|');
		sb.append(tripStopStatus.name()).append('|');
		sb.append(messageContent == null ? "\\0" : messageContent).append('|');
		sb.append(messageType == null ? "\\0" : messageType).append('|');
		sb.append(sideCode == null ? "-" : sideCode).append('|');
		sb.append(numberOfCoaches == null ? "\\0" : numberOfCoaches).append('|');
		sb.append(wheelChairAccessible == null ? WheelChairAccessible.UNKNOWN : wheelChairAccessible).append('|');
		sb.append(operatorCode == null ? "\\0" : operatorCode).append('|');

		sb.append(reasonType == null ? "\\0" : reasonType).append('|');
		sb.append(subReasonType == null ? "\\0" : subReasonType).append('|');
		sb.append(reasonContent == null ? "\\0" : reasonContent).append('|');

		sb.append(adviceType == null ? "\\0" : adviceType).append('|');
		sb.append(subAdviceType == null ? "\\0" : subAdviceType).append('|');
		sb.append(adviceContent == null ? "\\0" : adviceContent).append('|');

		sb.append(timingPointDataOwnerCode == null ? "\\0" : timingPointDataOwnerCode.name()).append('|');
		sb.append(timingPointCode == null ? "\\0" : timingPointCode).append('|');
		sb.append(journeyStopType == null ? "\\0" : journeyStopType.name()).append('|');

		String tta = to32Time(targetArrivalTime);
		sb.append(tta == null ? "\\0" : tta).append('|');
		String ttd = to32Time(targetDepartureTime);
		sb.append(ttd == null ? "\\0" : ttd).append('|');
		String rta = to32Time(recordedArrivalTime);
		sb.append(rta == null ? "\\0" : rta).append('|');
		String rtd = to32Time(recordedDepartureTime);
		sb.append(rtd == null ? "\\0" : rtd);
		return sb.toString();
	}


	public static DatedPasstime fromCtxLine(String line){
		String[] v = line.split("\\|");
		for (int j = 0; j < v.length; j++) {
			if ("\\0".equals(v[j])) {
				v[j] = null;
			}
		}
		DatedPasstime res = new DatedPasstime();
		res.setDataOwnerCode(DataOwnerCode.valueOf(v[0]));
		res.setOperationDate(v[1]);
		res.setLinePlanningNumber(v[2]);
		res.setJourneyNumber(Integer.valueOf(v[3]));
		res.setFortifyOrderNumber(Integer.valueOf(v[4]));
		res.setUserStopOrderNumber(Integer.valueOf(v[5]));
		res.setUserStopCode(v[6]);
		res.setLocalServiceLevelCode(Integer.valueOf(v[7]));
		res.setJourneyPatternCode(Integer.valueOf(v[8]));
		res.setLineDirection(Integer.valueOf(v[9]));
		res.setDestinationCode(v[11]);
		res.setTimingStop(Boolean.valueOf(v[12]));
		res.setExpectedArrivalTime(secondsSinceMidnight(v[13]));
		res.setExpectedDepartureTime(secondsSinceMidnight(v[14]));
		res.setTripStopStatus(TripStopStatus.valueOf(v[15]));
		res.setMessageContent(v[16]);
		if (v[17] != null)
			res.setMessageType(MessageType.valueOf(v[17]));
		res.setSideCode(v[18]);
		if (v[19] != null)
			res.setNumberOfCoaches(Integer.valueOf(v[19]));
		res.setWheelChairAccessible(WheelChairAccessible.valueOf(v[20]));
		res.setOperatorCode(v[21]);
		if (v[22] != null)
			res.setReasonType(v[22]);
		if (v[23] != null)
			res.setSubReasonType(v[23]);
		res.setReasonContent(v[24]);
		if (v[25] != null){
			res.setAdviceType(v[25]);
		}
		if (v[26] != null)
			res.setSubAdviceType(v[26]);
		res.setAdviceContent(v[27]);
		res.setTimingPointDataOwnerCode(DataOwnerCode.valueOf(v[28]));
		res.setTimingPointCode(v[29]);
		res.setJourneyStopType(JourneyStopType.valueOf(v[30]));
		res.setTargetArrivalTime(secondsSinceMidnight(v[31]));
		res.setTargetDepartureTime(secondsSinceMidnight(v[32]));
		res.setLastUpdateTimeStamp(DateUtils.parse(v[10]));
		return res;
	}


	public static ArrayList<DatedPasstime> fromCtx(String ctx){
		ArrayList<DatedPasstime> result = new ArrayList<DatedPasstime>();
		for (String line : ctx.split("\r\n")){
			if (line.charAt(0) == '\\'){
				continue;
			}
			result.add(fromCtxLine(line));
		}
		return result;
	}

	private final static String HEADER = "\\GKV8turbo_passtimes|KV8turbo_passtimes|%s|||UTF-8|0.1|%s|\r\n"
			+ "\\TDATEDPASSTIME|DATEDPASSTIME|start object\r\n"
			+ "\\LDataOwnerCode|OperationDate|LinePlanningNumber|JourneyNumber|FortifyOrderNumber|UserStopOrderNumber|UserStopCode|LocalServiceLevelCode|JourneyPatternCode|LineDirection|LastUpdateTimeStamp|DestinationCode|IsTimingStop|ExpectedArrivalTime|ExpectedDepartureTime|TripStopStatus|MessageContent|MessageType|SideCode|NumberOfCoaches|WheelChairAccessible|OperatorCode|ReasonType|SubReasonType|ReasonContent|AdviceType|SubAdviceType|AdviceContent|TimingPointDataOwnerCode|TimingPointCode|JourneyStopType|TargetArrivalTime|TargetDepartureTime|RecordedArrivalTime|RecordedDepartureTime\r\n";

	public static String header(String subscription){
		SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		return String.format(HEADER,subscription,iso8601.format(new Date()));
	}
}
