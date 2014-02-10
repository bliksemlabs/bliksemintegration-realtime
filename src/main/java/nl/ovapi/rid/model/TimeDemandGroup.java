package nl.ovapi.rid.model;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode()
public class TimeDemandGroup {

	public static class TimeDemandGroupPoint {
		@Getter
		@Setter
		/**
		 * Sequential order in JourneyPattern and TimeDemandGroup
		 */
		public Integer pointorder;
		@Getter
		@Setter
		/**
		 * Seconds since start of trip
		 */
		public Integer totaldrivetime;
		@Getter
		@Setter
		/**
		 * Seconds between arrivaltime and departuretime at this stop.
		 */
		public Integer stopwaittime;
	}

	/**
	 * List of points in TimeDemandGroup
	 */
	@Getter private ArrayList<TimeDemandGroupPoint> points;

	/**
	 * Add point to TimeDemandGroup
	 * @param point
	 */
	public void add(TimeDemandGroupPoint point){
		points.add(point);
	}
	
	public TimeDemandGroup() {
		points = new ArrayList<TimeDemandGroupPoint>();
	}
}
