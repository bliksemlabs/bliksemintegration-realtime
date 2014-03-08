package nl.ovapi.rid.gtfsrt;

import javax.xml.datatype.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static final Logger _log = LoggerFactory.getLogger(Utils.class);

	public static long currentTimeSecs(){
		return System.currentTimeMillis()/1000;
	}
	
	public static int toSeconds(Duration d){
		return d == null ? 0 : d.getSeconds() + d.getMinutes()*60+ d.getHours()*60*60+d.getDays()*24*60*60;
	}
}
