package nl.ovapi.bison.model;

import java.util.ArrayList;

import com.google.common.base.Objects;

import nl.ovapi.bison.sax.DateUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
	@Getter private ReasonType reasonType;

	@Getter private SubReasonType subReasonType;
	@Getter private String reasonContent;
	@Getter private AdviceType adviceType;
	@Getter private SubAdviceType subAdviceType;
	@Getter private String adviceContent;
	@Getter private DataOwnerCode timingPointDataOwnerCode;
	@Getter private String timingPointCode;
	@Getter private JourneyStopType journeyStopType;
	@Getter private Integer targetArrivalTime;
	@Getter private Integer targetDepartureTime;
	@Getter private Integer recordedArrivalTime;
	@Getter private Integer recordedDepartureTime;

	public void setReasonType(ReasonType reasonType) {
		if (!Objects.equal(reasonType, this.reasonType)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.reasonType = reasonType;
	}

	public void setOperatorCode(String operatorCode) {
		if (!Objects.equal(operatorCode, this.operatorCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.operatorCode = operatorCode;
	}

	public void setWheelChairAccessible(WheelChairAccessible wheelChairAccessible) {
		if (!Objects.equal(wheelChairAccessible, this.wheelChairAccessible)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.wheelChairAccessible = wheelChairAccessible;
	}

	public void setNumberOfCoaches(Integer numberOfCoaches) {
		if (!Objects.equal(numberOfCoaches, this.numberOfCoaches)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.numberOfCoaches = numberOfCoaches;
	}

	public void setSideCode(String sideCode) {
		if (!Objects.equal(sideCode, this.sideCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.sideCode = sideCode;
	}

	public void setMessageType(MessageType messageType) {
		if (!Objects.equal(messageType, this.messageType)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.messageType = messageType;
	}

	public void setMessageContent(String messageContent) {
		if (!Objects.equal(messageContent, this.messageContent)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.messageContent = messageContent;
	}

	public void setTripStopStatus(TripStopStatus tripStopStatus) {
		if (!Objects.equal(tripStopStatus, this.tripStopStatus)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.tripStopStatus = tripStopStatus;
	}

	public void setExpectedDepartureTime(Integer expectedDepartureTime) {
		if (!Objects.equal(expectedDepartureTime, this.expectedDepartureTime)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.expectedDepartureTime = expectedDepartureTime;
	}

	public void setExpectedArrivalTime(Integer expectedArrivalTime) {
		if (!Objects.equal(expectedArrivalTime, this.expectedArrivalTime)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.expectedArrivalTime = expectedArrivalTime;
	}

	public void setTimingStop(boolean isTimingStop) {
		if (!Objects.equal(isTimingStop, this.isTimingStop)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.isTimingStop = isTimingStop;
	}

	public void setDestinationCode(String destinationCode) {
		if (!Objects.equal(destinationCode, this.destinationCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.destinationCode = destinationCode;
	}

	public void setLastUpdateTimeStamp(Long lastUpdateTimeStamp) {
		this.lastUpdateTimeStamp = lastUpdateTimeStamp;
	}

	public void setLineDirection(Integer lineDirection) {
		if (!Objects.equal(lineDirection, this.lineDirection)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.lineDirection = lineDirection;
	}

	public void setJourneyPatternCode(Integer journeyPatternCode) {
		if (!Objects.equal(journeyPatternCode, this.journeyPatternCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.journeyPatternCode = journeyPatternCode;
	}

	public void setLocalServiceLevelCode(Integer localServiceLevelCode) {
		if (!Objects.equal(localServiceLevelCode, this.localServiceLevelCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.localServiceLevelCode = localServiceLevelCode;
	}

	public void setUserStopCode(String userStopCode) {
		if (!Objects.equal(userStopCode, this.userStopCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.userStopCode = userStopCode;
	}

	public void setUserStopOrderNumber(Integer userStopOrderNumber) {
		if (!Objects.equal(userStopOrderNumber, this.userStopOrderNumber)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.userStopOrderNumber = userStopOrderNumber;
	}

	public void setFortifyOrderNumber(Integer fortifyOrderNumber) {
		if (!Objects.equal(fortifyOrderNumber, this.fortifyOrderNumber)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.fortifyOrderNumber = fortifyOrderNumber;
	}

	public void setJourneyNumber(Integer journeyNumber) {
		if (!Objects.equal(journeyNumber, this.journeyNumber)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.journeyNumber = journeyNumber;
	}

	public void setLinePlanningNumber(String linePlanningNumber) {
		if (!Objects.equal(linePlanningNumber, this.linePlanningNumber)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.linePlanningNumber = linePlanningNumber;
	}

	public void setOperationDate(String operationDate) {
		if (!Objects.equal(operationDate, this.operationDate)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.operationDate = operationDate;
	}

	public void setDataOwnerCode(DataOwnerCode dataOwnerCode) {
		if (!Objects.equal(dataOwnerCode, this.dataOwnerCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.dataOwnerCode = dataOwnerCode;
	}

	public void setSubReasonType(SubReasonType subReasonType) {
		if (!Objects.equal(subReasonType, this.subReasonType)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.subReasonType = subReasonType;
	}

	public void setReasonContent(String reasonContent){
		if (!Objects.equal(reasonContent, this.reasonContent)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.reasonContent = reasonContent;
	}

	public void setAdviceType(AdviceType adviceType){
		if (!Objects.equal(adviceType, this.adviceType)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.adviceType = adviceType;
	}	

	public void setSubAdviceType(SubAdviceType subAdviceType){
		if (!Objects.equal(subAdviceType, this.subAdviceType)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.subAdviceType = subAdviceType;
	}	

	public void setAdviceContent(String adviceContent){
		if (!Objects.equal(adviceContent, this.adviceContent)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.adviceContent = adviceContent;
	}	

	public void setTimingPointDataOwnerCode(DataOwnerCode timingPointDataOwnerCode){
		if (!Objects.equal(timingPointDataOwnerCode, this.timingPointDataOwnerCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.timingPointDataOwnerCode = timingPointDataOwnerCode;
	}

	public void setTimingPointCode(String timingPointCode){
		if (!Objects.equal(timingPointCode, this.timingPointCode)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.timingPointCode = timingPointCode;
	}

	public void setJourneyStopType(JourneyStopType journeyStopType){
		if (!Objects.equal(journeyStopType, this.journeyStopType)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.journeyStopType = journeyStopType;
	}

	public void setTargetDepartureTime(Integer targetDepartureTime){
		if (!Objects.equal(targetDepartureTime, this.targetDepartureTime)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.targetDepartureTime = targetDepartureTime;
	}

	public void setTargetArrivalTime(Integer targetArrivalTime){
		if (!Objects.equal(targetArrivalTime, this.targetArrivalTime)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.targetArrivalTime = targetArrivalTime;
	}

	public void setRecordedDepartureTime(Integer recordedDepartureTime){
		if (!Objects.equal(recordedDepartureTime, this.recordedDepartureTime)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
		}
		this.recordedDepartureTime = recordedDepartureTime;
	}

	public void setRecordedArrivalTime(Integer recordedArrivalTime){
		if (!Objects.equal(recordedArrivalTime, this.recordedArrivalTime)){
			this.setLastUpdateTimeStamp(System.currentTimeMillis());
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
			res.setReasonType(ReasonType.parse(v[22]));
		if (v[23] != null)
			res.setSubReasonType(SubReasonType.parse(v[23]));
		res.setReasonContent(v[24]);
		if (v[25] != null){
			res.setAdviceType(AdviceType.parse(v[25]));
		}
		if (v[26] != null)
			res.setSubAdviceType(SubAdviceType.parse(v[26]));
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
}
