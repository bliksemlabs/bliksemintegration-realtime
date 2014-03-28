package nl.ovapi.bison.model;
public enum SubAdviceType{
	NONE,DO_NOT_TRAVEL,TRAVEL_WITH_OTHER,TRANSFER_IN,TRAVEL_VIA,BOARD_EXIT;

	public static SubAdviceType parse(String value){
		if (value == null)
			return null;
		if ("0".equals(value))
			return NONE;
		else if ("1".equals(value))
			return DO_NOT_TRAVEL;
		else if ("2".equals(value))
			return TRAVEL_WITH_OTHER;
		else if ("3_1".equals(value))
			return TRANSFER_IN;
		else if ("3_2".equals(value))
			return TRAVEL_VIA;
		else if ("3_3".equals(value))
			return BOARD_EXIT;	
		else return null;
	}
}
