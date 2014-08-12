package nl.ovapi.rid.model;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import com.google.common.collect.ImmutableList;

@ToString()
@EqualsAndHashCode()
public class JourneyPattern implements Cloneable{
	
	@Getter
	/**
	 * DirectionType of JourneyPattern within Line. 1 or 2.
	 */
	public final Integer directiontype;
	
	@ToString()
	public static class JourneyPatternPoint implements Cloneable{
		
		private JourneyPatternPoint(JourneyPatternPoint toClone){
			this.added = toClone.added;
			this.distancefromstartroute = toClone.distancefromstartroute == null ? null : toClone.distancefromstartroute.intValue();
			this.operatorpointref = toClone.operatorpointref;
			this.pointorder = toClone.pointorder == null ? null : toClone.pointorder.intValue();
			this.pointref = toClone.pointref == null ? null : toClone.pointref.longValue();
			this.scheduled = toClone.scheduled;
			this.skipped = toClone.skipped;
			this.waitpoint = toClone.waitpoint;
			this.destinationCode = toClone.destinationCode;
			this.platformCode = toClone.platformCode;
			this.forBoarding = toClone.forBoarding;
			this.forAlighting = toClone.forAlighting;
		}
		
		public Builder edit(){
			return new Builder(new JourneyPatternPoint(this));
		}
		
		private JourneyPatternPoint(Integer pointorder, Long pointref,
				String operatorpointref, Integer distancefromstartroute,
				boolean scheduled, boolean skipped, boolean waitpoint,
				String destinationCode, String platformCode,boolean added,boolean forBoarding, boolean forAlighting) {
			this.added = added;
			this.distancefromstartroute = distancefromstartroute;
			this.operatorpointref = operatorpointref;
			this.pointorder = pointorder;
			this.pointref = pointref;
			this.scheduled = scheduled;
			this.skipped = skipped;
			this.waitpoint = waitpoint;
			this.destinationCode = destinationCode;
			this.platformCode = platformCode;
			this.forBoarding = forBoarding;
			this.forAlighting = forAlighting;
		}

		@Override
		public JourneyPatternPoint clone(){
			return new JourneyPatternPoint(this);
		}
				
		public static class Builder{
			private Integer pointorder;
			private Builder(){}
			private Builder(JourneyPatternPoint toClone) {
				this.added = toClone.added;
				this.distancefromstartroute = toClone.distancefromstartroute == null ? null : toClone.distancefromstartroute.intValue();
				this.operatorpointref = toClone.operatorpointref;
				this.pointorder = toClone.pointorder == null ? null : toClone.pointorder.intValue();
				this.pointref = toClone.pointref == null ? null : toClone.pointref.longValue();
				this.scheduled = toClone.scheduled;
				this.skipped = toClone.skipped;
				this.waitpoint = toClone.waitpoint;
				this.destinationCode = toClone.destinationCode;
				this.platformCode = toClone.platformCode;
				this.forBoarding = toClone.forBoarding;
				this.forAlighting = toClone.forAlighting;
			}

			/**
			 * Set sequential order in JourneyPattern and TimeDemandGroup
			 */
			public Builder setPointOrder(@NonNull Integer pointorder){
				this.pointorder = pointorder;
				return this;
			}

			private Long pointref;
			/**
			 * Set reference to this point in RID database.
			 */
			public Builder setPointRef(@NonNull Long pointref){
				this.pointref = pointref;
				return this;
			}
			@NonNull

			private String operatorpointref;
			/**
			 * Set DataOwnerCode:UserStopCode of StopPoint.
			 */
			public Builder setOperatorPointRef(@NonNull String operatorpointref){
				this.operatorpointref = operatorpointref;
				return this;
			}
			@NonNull
			private boolean waitpoint = false;
			/**
			 * Set whether this point is a WaitPoint/TimingPoint, if true a vehicle is not expected to depart early from this stop
			 */
			public Builder setIsWaitpoint(@NonNull boolean waitpoint){
				this.waitpoint = waitpoint;
				return this;
			}
			
			
			@NonNull
			private Integer distancefromstartroute;
			/**
			 * Set distance from start route
			 */
			public Builder setDistanceFromStartRoute(@NonNull Integer distancefromstartroute){
				this.distancefromstartroute = distancefromstartroute;
				return this;
			}
			
			@Getter
			@NonNull
			private boolean scheduled = true;
			/**
			 * Set whether stop is scheduled (not a dummy stop such as a bridge)
			 */
			public Builder setIsScheduled(@NonNull boolean isScheduled){
				this.scheduled = isScheduled;
				return this;
			}

			@Getter
			@NonNull
			private boolean skipped =false;
			/**
			 * Set whether stop is skipped (canceled)
			 */
			public Builder setIsSkipped(@NonNull boolean isSkipped){
				this.skipped = isSkipped;
				return this;
			}

			@Getter
			@NonNull
			/**
			 * Set whether this point is added on the planned schedule.
			 */
			private Boolean added = false;
			public Builder setIsAdded(@NonNull boolean isAdded){
				this.added = isAdded;
				return this;
			}
			
			@Getter
			private String destinationCode;
			/**
			 * Set DestinationCode as set in Koppelvlak1;
			 */
			public Builder setDestinationCode(@NonNull String destinationCode){
				this.destinationCode = destinationCode;
				return this;
			}
			
			@Getter
			private String platformCode;
			/**
			 * Set PlatformCode / sideCode
			 */
			public Builder setPlatformCode(String platformCode){
				this.platformCode = platformCode;
				return this;
			}
			
			@Getter
			private boolean forBoarding = true;
			/**
			 * Set PlatformCode / sideCode
			 */
			public Builder setForBoarding(boolean forBoarding){
				this.forBoarding = forBoarding;
				return this;
			}
			
			@Getter
			private boolean forAlighting = true;
			/**
			 * Set PlatformCode / sideCode
			 */
			public Builder setForAlighting(boolean forAlighting){
				this.forAlighting = forAlighting;
				return this;
			}
			
			
			public JourneyPatternPoint build(){
				return new JourneyPatternPoint(pointorder,pointref,operatorpointref,distancefromstartroute,scheduled,skipped,waitpoint,destinationCode,platformCode,added,forBoarding,forAlighting);
			}
		}
		
		public static Builder newBuilder(){
			return new Builder();
		}
		
		@Getter
		@NonNull
		/**
		 * Sequential order in JourneyPattern and TimeDemandGroup
		 */
		private final Integer pointorder;
		@Getter
		@NonNull
		/**
		 * Reference to this point in RID database.
		 */
		private final Long pointref;
		@Getter
		@NonNull
		/**
		 * DataOwnerCode:UserStopCode of StopPoint.
		 */
		private final String operatorpointref;
		@Getter
		@NonNull
		/**
		 * Whether this point is a WaitPoint/TimingPoint, if true a vehicle is not expected to depart early from this stop
		 */
		private final boolean waitpoint;
		@Getter
		@NonNull
		/**
		 * Distance in meters from start of route. NOTE: does not have to start at 0.
		 */
		private final Integer distancefromstartroute;
		@Getter
		@NonNull
		/**
		 * If stoppoint is not Scheduled, its a dummy not meant for passengers such as bridges and KAR points.
		 * Dummies are included in the case a KV6 messages arrives on a dummy.
		 */
		private final boolean scheduled;

		@Getter
		@NonNull
		/**
		 * This point is skipped.
		 */
		private final boolean skipped;

		@Getter
		@NonNull
		/**
		 * This point is added on the planned schedule.
		 */
		private final boolean added;
		
		@Getter
		/**
		 * DestinationCode as set in Koppelvlak1;
		 */
		private final String destinationCode;
		
		@Getter
		/**
		 * PlatformCode / sideCode;
		 */
		private final String platformCode;
		
		@Getter
		/**
		 * is boarding allowed at stop;
		 */
		private final boolean forBoarding;

		@Getter
		/**
		 * is alighting allowed at stop;
		 */
		private final boolean forAlighting;

	}
	
	/**
	 * @param userstopcode DataOwnerCode':'UserStopCode
	 * @return false (JourneyPattern does not contain point), true (JourneyPattern contains point)
	 */
	public boolean contains(@NonNull String userstopcode) {
		for (JourneyPatternPoint pt : points) {
			if (userstopcode.equals(pt.getOperatorpointref())) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param pointOrder Sequential order to be retrievd
	 * @return NULL: PointOrder not present in JourneyPattern
	 *         StopPoint with given PointOrder
	 */
	
	public JourneyPatternPoint getPoint(int pointOrder){
		for (JourneyPatternPoint pt : points){
			if (pt.pointorder.equals(pointOrder)){
				return pt;
			}
		}
		return null;
	}
	
	/**
	 * List with StopPoints in JourneyPattern
	 */
	@Getter private final ImmutableList<JourneyPatternPoint> points;
	
	@Getter private final String journeyPatternRef;
	
	private JourneyPattern(String journeyPatternRef,Integer directionType,@NonNull List<JourneyPatternPoint> points){
		this.journeyPatternRef = journeyPatternRef;
		this.directiontype = directionType;
		ImmutableList.Builder<JourneyPatternPoint> builder = ImmutableList.builder();
		builder.addAll(points);
		this.points = builder.build();
	}


	public static class Builder{
		private ArrayList<JourneyPatternPoint> points;
		
		public Builder(){
			points = new ArrayList<JourneyPatternPoint>();
		}
		
		public Integer directiontype;

		/**
		 * Set directionType of JourneyPattern within Line. 1 or 2.
		 */
		public void setDirectionType(Integer directionType){
			this.directiontype = directionType;
		}

		@Getter private String journeyPatternRef;

		public void setJourneyPatternref(String journeyPatternRef){
			this.journeyPatternRef = journeyPatternRef;
		}


		public Builder(JourneyPattern journeyPattern){
			this();
			this.directiontype = journeyPattern.getDirectiontype();
			this.journeyPatternRef = journeyPattern.getJourneyPatternRef();
			for (JourneyPatternPoint point : journeyPattern.getPoints()){
				points.add(new JourneyPatternPoint(point));
			}
		}

		/**
		 * Add point to TimeDemandGroup
		 * @param point
		 */
		public void add(JourneyPatternPoint point){
			this.points.add(point);
		}

		/**
		 * Inserts the specified TimeDemandGroupPoint point at the specified position in this list
		 * @param index
		 * @param point
		 */
		public void add(int index,JourneyPatternPoint point){
			this.points.add(index,point);
		}
	
		public JourneyPattern build(){
			return new JourneyPattern(journeyPatternRef,directiontype,this.points);
		}
	}
	
	public static Builder newBuilder(){
		return new Builder();
	}
	
	public Builder edit(){
		return new Builder(this);
	}
	
	public JourneyPattern clone() {
	    return edit().build();
	}
}
