package nl.ovapi.bison.model;
public enum ReasonType{
	UNKNOWN,GENERAL,UNDEF;

	public static ReasonType parse(String value){
		if (value == null)
			return null;
		if ("0".equals(value))
			return UNKNOWN;
		else if ("1".equals(value))
			return GENERAL;
		else if ("255".equals(value))
			return UNDEF;
		else return UNKNOWN;
	}
}