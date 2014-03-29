package nl.ovapi.rid.model;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode()
public class TimeDemandGroup implements Cloneable{

	public TimeDemandGroup (TimeDemandGroup toClone) {
		this.points = new ArrayList<TimeDemandGroupPoint>();
		for (TimeDemandGroupPoint pt : toClone.points){
			this.points.add(pt.clone());
		}
	}
	
	public TimeDemandGroup clone() {
	    return new TimeDemandGroup(this);
	}
	
	@ToString
	public static class TimeDemandGroupPoint implements Cloneable{
		
		public TimeDemandGroupPoint(TimeDemandGroupPoint toClone){
			this.pointorder = toClone.pointorder == null ? null : toClone.pointorder.intValue();
			this.stopwaittime = toClone.stopwaittime == null ? null : toClone.stopwaittime.intValue();
			this.totaldrivetime = toClone.totaldrivetime == null ? null : toClone.totaldrivetime.intValue();
		}

		
		@Override
		public TimeDemandGroupPoint clone(){
			return new TimeDemandGroupPoint(this);
		}
		
		public TimeDemandGroupPoint(){}

		
		@Getter
		@Setter
		/**
		 * Sequential order in JourneyPattern and TimeDemandGroup
		 */
		private Integer pointorder;
		@Getter
		@Setter
		/**
		 * Seconds since start of trip
		 */
		private Integer totaldrivetime;
		@Getter
		@Setter
		/**
		 * Seconds between arrivaltime and departuretime at this stop.
		 */
		private Integer stopwaittime;
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
