package nl.ovapi.bison.model;

public enum SubEffectType{
	UNKNOWN,DECREASED_SERVICE,NO_SERVICE,DISRUPTED,NO_TRAINS,DIVERSION,DELAYED_DIVERSION,DELAY_UNKNOWN,
	DELAY_5,DELAY_10,DELAY_15,DELAY_30,DELAY_45,DELAY_60,DELAY_60PLUS,DELAY_510,DELAY_1015,DELAY_1530,DELAY_3060,
	STOPCANCEL,LINECANCEL;

	@Override
	public String toString(){
		switch(this){
		case UNKNOWN:
			return "Onbekend";
		case DECREASED_SERVICE:
			return "Minder vervoer";
		case NO_SERVICE:
			return "Geen vervoer";
		case DISRUPTED:
			return "Vervoer ontregeld";
		case NO_TRAINS:
			return "Geen treinen";
		case DIVERSION:
			return "Omleiding";
		case DELAYED_DIVERSION:
			return "Omleiding met vertraging";
		case DELAY_UNKNOWN:
			return "Onbekende vertraging";
		case DELAY_5:
			return "5 minuten vertraging";
		case DELAY_10:
			return "10 minuten vertraging";
		case DELAY_15:
			return "15 minuten vertraging";
		case DELAY_30:
			return "30 minuten vertraging";
		case DELAY_45:
			return "45 minuten vertraging";
		case DELAY_60:
			return "60 minuten vertraging";
		case DELAY_60PLUS:
			return "60 minuten of meer vetraging";
		case DELAY_510:
			return "5 tot 10 minuten vertraging";
		case DELAY_1015:
			return "10 tot 15 minuten vertraging";
		case DELAY_1530:
			return "15 tot 30 minuten vertraging";
		case DELAY_3060:
			return "30 tot 60 minuten vertraging";
		case STOPCANCEL:
			return "Vervallen halte(n)";
		case LINECANCEL:
			return "Traject vervallen";
		default:
			return null;
		}
	}

	public static SubEffectType parse(String value){
		if ("0".equals(value)){
			return UNKNOWN;
		}else if ("11".equals(value)){
			return DECREASED_SERVICE;
		}else if ("5".equals(value)){
			return NO_SERVICE;
		}else if ("6".equals(value)){
			return DISRUPTED;
		}else if ("4".equals(value)){
			return DIVERSION;
		}else if ("4_1".equals(value)){
			return DELAYED_DIVERSION;
		}else if ("3_1".equals(value)){
			return DELAY_UNKNOWN;
		}else if ("3_2".equals(value)){
			return DELAY_5;
		}else if ("3_3".equals(value)){
			return DELAY_10;
		}else if ("3_4".equals(value)){
			return DELAY_15;
		}else if ("3_5".equals(value)){
			return DELAY_30;
		}else if ("3_6".equals(value)){
			return DELAY_45;
		}else if ("3_7".equals(value)){
			return DELAY_60;
		}else if ("3_8".equals(value)){
			return DELAY_60PLUS;
		}else if ("3_9".equals(value)){
			return DELAY_510;
		}else if ("3_10".equals(value)){
			return DELAY_1015;
		}else if ("3_11".equals(value)){
			return DELAY_1530;
		}else if ("3_12".equals(value)){
			return DELAY_3060;
		}else if ("5_1".equals(value)){
			return STOPCANCEL;
		}else if ("5_2".equals(value)){
			return LINECANCEL;
		}else{
			return UNKNOWN;
		}
	}

}
