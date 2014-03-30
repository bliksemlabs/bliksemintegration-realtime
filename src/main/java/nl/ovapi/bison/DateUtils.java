package nl.ovapi.bison;

import java.text.SimpleDateFormat;

public class DateUtils {

	/**
	 * @param time ISO8601 representation of time eg. yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd HH:mm:ss
	 * @return POSIX time seconds since January 1st 1970 00:00:00 UTC
	 */
	public static Long parse(String time) {
		if (time == null || time.length() < 3){
			return null;
		}
		// NOTE: SimpleDateFormat uses GMT[-+]hh:mm for the TZ which breaks
		// things a bit. Before we go on we have to repair this.
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		if (time.charAt(10) != 'T'){
			df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
		try {
			return df.parse(time).getTime()/1000;
		} catch (Exception e) {
			return null;
		}
	}

	public static String to32Time(Integer secondsSinceMidnight) {
		if (secondsSinceMidnight == null){
			return null;
		}
		int hours = secondsSinceMidnight/3600;
		int seconds = secondsSinceMidnight % 3600;
		int minutes = seconds / 60;
		seconds = seconds % 60;
		return String.format("%02d:%02d:%02d",hours,minutes,seconds);
	}
}
