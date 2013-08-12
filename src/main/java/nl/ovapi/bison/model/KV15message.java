package nl.ovapi.bison.model;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString()
public class KV15message {
	@Getter
	@Setter
	private Boolean isDelete;
	@Getter
	@Setter
	private DataOwnerCode dataOwnerCode;
	@Getter
	@Setter
	private String messageCodeDate;
	@Getter
	@Setter
	private Integer messageCodeNumber;
	@Getter
	private ArrayList<String> userstopCodes;
	@Getter
	private ArrayList<String> linePlanningNumbers;

	public void addUserstopCode(String userStopCode){
		userstopCodes.add(userStopCode);
	}
	
	public void addLinePlanningNumber(String linePlanningNumber){
		linePlanningNumbers.add(linePlanningNumber);
	}
	
	
	public KV15message(){
		userstopCodes = new ArrayList<String>();
		linePlanningNumbers = new ArrayList<String>();
	}
	
	@Getter
	@Setter
	private MessagePriority messagePriority;

	@Getter
	@Setter
	private MessageType messageType;

	@Getter
	@Setter
	private MessageDurationType messageDurationType;

	@Getter
	@Setter
	private Long messageStartTime;

	@Getter
	@Setter
	private Long messageEndTime;

	@Getter
	@Setter
	private String messageContent;
	
	@Getter
	@Setter
	private ReasonType reasonType;

	@Getter
	@Setter
	private SubReasonType subReasonType;

	@Getter
	@Setter
	private String reasonContent;

	@Getter
	@Setter
	private EffectType effectType;
	
	@Getter
	@Setter
	private SubEffectType subEffectType;
	
	@Getter
	@Setter
	private String effectContent;

	
	@Getter
	@Setter
	private AdviceType adviceType;
	
	@Getter
	@Setter
	private SubAdviceType subAdviceType;
	
	@Getter
	@Setter
	private String adviceContent;
	
	@Getter
	@Setter
	private Integer measureType;
	
	@Getter
	@Setter
	private SubMeasureType subMeasureType;
	
	@Getter
	@Setter
	private String measureContent;


	@Getter
	@Setter
	private long messageTimeStamp;

}
