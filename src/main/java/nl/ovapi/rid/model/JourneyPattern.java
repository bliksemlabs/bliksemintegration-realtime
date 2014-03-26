package nl.ovapi.rid.model;

import java.util.ArrayList;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@ToString()
@EqualsAndHashCode()
public class JourneyPattern implements Cloneable{

	public JourneyPattern (JourneyPattern toClone) {
		this.directiontype = toClone.directiontype == null ? null : toClone.directiontype.intValue();
		this.journeyPatternRef = toClone.journeyPatternRef;
		this.points = new ArrayList<JourneyPatternPoint>(toClone.getPoints().size());
		for (JourneyPatternPoint pt : toClone.getPoints()){
			this.points.add(pt.clone());
		}
	}
	
	public JourneyPattern clone() {
	    return new JourneyPattern(this);
	}
	
	@Getter
	@Setter
	/**
	 * DirectionType of JourneyPattern within Line. 1 or 2.
	 */
	public Integer directiontype;
	
	@ToString()
	public static class JourneyPatternPoint implements Cloneable{
		
		public  JourneyPatternPoint(JourneyPatternPoint toClone){
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
		}
		
		@Override
		public JourneyPatternPoint clone(){
			return new JourneyPatternPoint(this);
		}
		
		public JourneyPatternPoint(){}
		
		@Getter
		@Setter
		@NonNull
		/**
		 * Sequential order in JourneyPattern and TimeDemandGroup
		 */
		private Integer pointorder;
		@Getter
		@Setter
		@NonNull
		/**
		 * Reference to this point in RID database.
		 */
		private Long pointref;
		@Getter
		@Setter
		@NonNull
		/**
		 * DataOwnerCode:UserStopCode of StopPoint.
		 */
		private String operatorpointref;
		@Getter
		@Setter
		@NonNull
		/**
		 * Whether this point is a WaitPoint/TimingPoint, if true a vehicle is not expected to depart early from this stop
		 */
		private boolean waitpoint;
		@Getter
		@Setter
		@NonNull
		/**
		 * Distance in meters from start of route. NOTE: does not have to start at 0.
		 */
		private Integer distancefromstartroute;
		@Getter
		@Setter
		@NonNull
		/**
		 * If stoppoint is not Scheduled, its a dummy not meant for passengers such as bridges and KAR points.
		 * Dummies are included in the case a KV6 messages arrives on a dummy.
		 */
		private boolean scheduled;

		@Getter
		@Setter
		@NonNull
		/**
		 * This point is skipped.
		 */
		private boolean skipped = false;

		@Getter
		@Setter
		@NonNull
		/**
		 * This point is added on the planned schedule.
		 */
		private boolean added = false;
		
		@Getter
		@Setter
		/**
		 * DestinationCode as set in Koppelvlak1;
		 */
		private String destinationCode;
		
		@Getter
		@Setter
		/**
		 * PlatformCode / sideCode;
		 */
		private String platformCode;
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
	@Getter private ArrayList<JourneyPatternPoint> points;
	
	@Getter @Setter private String journeyPatternRef;
	
	/**
	 * Add point to JourneyPattern
	 * @param point
	 */
	public void add(JourneyPatternPoint point){
		points.add(point);
	}

	public JourneyPattern() {
		points = new ArrayList<JourneyPatternPoint>();
	}

}
