package nl.ovapi.bison.model;
public enum AdviceType{
	UNKNOWN,GENERAL,UNDEF,NULL;

	public static AdviceType parse(String value){
		if (value == null)
			value = "";
		if ("0".equals(value))
			return UNKNOWN;
		else if ("1".equals(value))
			return GENERAL;
		else if ("255".equals(value))
			return UNDEF;
		else return UNKNOWN;
	}
}