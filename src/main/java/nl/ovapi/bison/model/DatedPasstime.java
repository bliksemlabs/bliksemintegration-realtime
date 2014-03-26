package nl.ovapi.bison.model;

import java.util.ArrayList;

import nl.ovapi.bison.sax.DateUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString()
public class DatedPasstime {

	@Getter @Setter private DataOwnerCode dataOwnerCode;
	@Getter @Setter private String operationDate;
	@Getter @Setter private String linePlanningNumber;
	@Getter @Setter private Integer journeyNumber;
	@Getter @Setter private Integer fortifyOrderNumber;
	@Getter @Setter private Integer UserStopOrderNumber;
	@Getter @Setter private String userStopCode;
	@Getter @Setter private Integer localServiceLevelCode;
	@Getter @Setter private Integer journeyPatternCode;
	@Getter @Setter private Integer lineDirection;
	@Getter @Setter private Long lastUpdateTimeStamp;
	@Getter @Setter private String destinationCode;
	@Getter @Setter private boolean isTimingStop;
	@Getter @Setter private Integer expectedArrivalTime;
	@Getter @Setter private Integer expectedDepartureTime;
	@Getter @Setter private TripStopStatus tripStopStatus;
	@Getter @Setter private String messageContent;
	@Getter @Setter private MessageType messageType;
	@Getter @Setter private String sideCode;
	@Getter @Setter private Integer numberOfCoaches;
	@Getter @Setter private WheelChairAccessible wheelChairAccessible;
	@Getter @Setter private String operatorCode;
	@Getter @Setter private ReasonType reasonType;
	@Getter @Setter private SubReasonType subReasonType;
	@Getter @Setter private String reasonContent;
	@Getter @Setter private AdviceType adviceType;
	@Getter @Setter private SubAdviceType subAdviceType;
	@Getter @Setter private String adviceContent;
	@Getter @Setter private DataOwnerCode timingPointDataOwnerCode;
	@Getter @Setter private String timingPointCode;
	@Getter @Setter private JourneyStopType journeyStopType;
	@Getter @Setter private Integer targetArrivalTime;
	@Getter @Setter private Integer targetDepartureTime;
	@Getter @Setter private Integer recordedArrivalTime;
	@Getter @Setter private Integer recordedDepartureTime;

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
		res.setLastUpdateTimeStamp(DateUtils.parse(v[10]));
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
