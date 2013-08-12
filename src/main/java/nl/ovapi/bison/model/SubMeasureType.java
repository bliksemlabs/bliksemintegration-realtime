package nl.ovapi.bison.model;
public enum SubMeasureType{
	EXTRA_TRANSPORT,CANCELLED_STOPS,SPECIAL_STOP,DIVERSION,
	NO_BUS,LIMITED_BUS,BUS,NO_TRAIN,LIMITED_TRAIN,DIVERTED_TRAIN,NONE,ROUTEMODIFIED,UNKNOWN;

	@Override
	public String toString(){
		switch(this){
		case EXTRA_TRANSPORT:
			return "Extra vervoer";
		case CANCELLED_STOPS:
			return "Vervallen halte(n)";
		case SPECIAL_STOP:
			return "Vervangende halte(n)";
		case DIVERSION:
			return "Rijden via omweg";
		case NO_BUS:
			return "Geen businzet";
		case LIMITED_BUS:
			return "Beperkte businzet";
		case BUS:
			return "Businzet";
		case NO_TRAIN:
			return "Geen treinen";
		case LIMITED_TRAIN:
			return "Minder treinen";
		case DIVERTED_TRAIN:
			return "Treinen rijden via";
		case NONE:
			return "Geen";
		case ROUTEMODIFIED:
			return "Route aangepast";
		case UNKNOWN:
			return "Onbekend";
		default:
			return null;
		}
	}

	public static SubMeasureType parse(String value){
		if ("0".equals(value)){
			return EXTRA_TRANSPORT;
		}else if ("1".equals(value)){
			return CANCELLED_STOPS;
		}else if ("2".equals(value)){
			return SPECIAL_STOP;
		}else if ("3".equals(value)){
			return DIVERSION;
		}else if ("4_1".equals(value)){
			return NO_BUS;
		}else if ("4_2".equals(value)){
			return LIMITED_BUS;
		}else if ("4_3".equals(value)){
			return BUS;
		}else if ("5_1".equals(value)){
			return NO_TRAIN;
		}else if ("5_2".equals(value)){
			return LIMITED_TRAIN;
		}else if ("5_3".equals(value)){
			return DIVERTED_TRAIN;
		}else if ("6".equals(value)){
			return NONE;
		}else if ("7".equals(value)){
			return ROUTEMODIFIED;
		}else{
			return UNKNOWN;
		}
	}
}