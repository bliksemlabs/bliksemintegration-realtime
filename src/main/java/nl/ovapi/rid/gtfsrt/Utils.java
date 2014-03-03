package nl.ovapi.rid.gtfsrt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

	private static final Logger _log = LoggerFactory.getLogger(Utils.class);

	public static long currentTimeSecs(){
		return System.currentTimeMillis()/1000;
	}
	
}
