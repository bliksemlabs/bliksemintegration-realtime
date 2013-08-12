package nl.ovapi.bison.model;
public enum EffectType{
	UNKNOWN,OTHER,PERSONNEL,MATERIAL,ENVIRONMENT,UNDEF;

	public static EffectType parse(String value){
		if (value == null)
			value = "";
		if ("1".equals(value))
			return OTHER;
		else if ("2".equals(value))
			return PERSONNEL;
		else if ("3".equals(value))
			return MATERIAL;
		else if ("4".equals(value))
			return ENVIRONMENT;
		else if ("255".equals(value))
			return UNDEF;
		else return OTHER;
	}
}