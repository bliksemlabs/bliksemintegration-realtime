package nl.ovapi.rid.model;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import com.google.common.collect.ImmutableList;

@ToString(callSuper = true)
@EqualsAndHashCode()
public class TimeDemandGroup implements Cloneable{

	public TimeDemandGroup clone() {
		TimeDemandGroup.Builder group = newBuilder();;
		for (TimeDemandGroupPoint pt : points){
			TimeDemandGroupPoint point = TimeDemandGroup.TimeDemandGroupPoint.newBuilder()
					.setPointOrder(pt.getPointorder())
					.setTotalDriveTime(pt.getTotaldrivetime())
					.setStopWaitTime(pt.getStopwaittime()).build();
			group.add(point);
		}
		return group.build();
	}

	@ToString
	public static class TimeDemandGroupPoint implements Cloneable{

		public static class Builder{
			private short pointorder;
			private int totaldrivetime;
			private int stopwaittime;

			private Builder(){}

			/**
			 * @param pointorder Sequential order in JourneyPattern and TimeDemandGroup
			 * @return Builder instance
			 */
			public Builder setPointOrder(short pointorder){
				this.pointorder = pointorder;
				return this;
			}

			/**
			 * 
			 * @param totalDriveTime time since start Journey
			 * @return builder instance
			 */
			public Builder setTotalDriveTime(Integer totalDriveTime){
				this.totaldrivetime = totalDriveTime;
				return this;
			}

			/**
			 * @param stopWaitTime dwell time at stop
			 * @return builder instance
			 */
			public Builder setStopWaitTime(Integer stopWaitTime){
				this.stopwaittime = stopWaitTime;
				return this;
			}
			/**
			 * @return TimeDemandGroupPoint instance with content of builder
			 */
			public TimeDemandGroupPoint build(){
				return new TimeDemandGroupPoint(pointorder,totaldrivetime,stopwaittime);
			}
		}

		public static TimeDemandGroup.TimeDemandGroupPoint.Builder newBuilder(){
			return new Builder();
		}

		public TimeDemandGroupPoint(TimeDemandGroupPoint toClone){
			this.pointorder = toClone.pointorder == null ? null : toClone.pointorder.shortValue();
			this.stopwaittime = toClone.stopwaittime == null ? null : toClone.stopwaittime.intValue();
			this.totaldrivetime = toClone.totaldrivetime == null ? null : toClone.totaldrivetime.intValue();
		}

		public TimeDemandGroupPoint(@NonNull Short pointorder, @NonNull Integer totaldrivetime,@NonNull Integer stopwaittime) {
			this.pointorder = pointorder;
			this.totaldrivetime = totaldrivetime;
			this.stopwaittime = stopwaittime;
		}

		@Getter
		/**
		 * Sequential order in JourneyPattern and TimeDemandGroup
		 */
		private final Short pointorder;
		@Getter
		/**
		 * Seconds since start of trip
		 */
		private final Integer totaldrivetime;
		@Getter
		/**
		 * Seconds between arrivaltime and departuretime at this stop.
		 */
		private final Integer stopwaittime;
	}

	/**
	 * List of points in TimeDemandGroup
	 */
	@Getter final private ImmutableList<TimeDemandGroupPoint> points;


	public static class Builder{
		private ArrayList<TimeDemandGroupPoint> points;
		public Builder(){
			points = new ArrayList<TimeDemandGroupPoint>();
		}

		public Builder(TimeDemandGroup timeDemandGroup){
			this();
			for (TimeDemandGroupPoint point : timeDemandGroup.getPoints()){
				points.add(new TimeDemandGroupPoint(point));
			}
		}

		/**
		 * Add point to TimeDemandGroup
		 * @param point
		 */
		public void add(TimeDemandGroupPoint point){
			this.points.add(point);
		}

		/**
		 * Inserts the specified TimeDemandGroupPoint point at the specified position in this list
		 * @param index
		 * @param point
		 */
		public void add(int index,TimeDemandGroupPoint point){
			this.points.add(index,point);
		}

		public TimeDemandGroup build(){
			return new TimeDemandGroup(this.points);
		}
	}

	private TimeDemandGroup(@NonNull List<TimeDemandGroupPoint> points){
		ImmutableList.Builder<TimeDemandGroupPoint> builder = ImmutableList.builder();
		builder.addAll(points);
		this.points = builder.build();
	}

	public static Builder newBuilder(){
		return new Builder();
	}

	public Builder edit(){
		return new Builder(this);
	}
}
