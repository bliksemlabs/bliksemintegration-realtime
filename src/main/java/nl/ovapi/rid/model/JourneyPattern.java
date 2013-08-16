package nl.ovapi.rid.model;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString()
public class JourneyPattern {

	@Getter
	@Setter
	public Integer directiontype;
	
	@ToString()
	public static class JourneyPatternPoint {
		@Getter
		@Setter
		private Integer pointorder;
		@Getter
		@Setter
		private Long pointref;
		@Getter
		@Setter
		private String operatorpointref;
		@Getter
		@Setter
		public boolean waitpoint;
		@Getter
		@Setter
		private Integer distancefromstartroute;
		@Getter
		@Setter
		private boolean scheduled;

	}
	
	public boolean contains(String userstopcode) {
		for (JourneyPatternPoint pt : points) {
			if (userstopcode.equals(pt.getOperatorpointref())) {
				return true;
			}
		}
		return false;
	}
	
	public JourneyPatternPoint getPoint(int pointorder){
		for (JourneyPatternPoint pt : points){
			if (pt.pointorder.equals(pointorder)){
				return pt;
			}
		}
		return null;
	}

	public ArrayList<JourneyPatternPoint> points;

	public JourneyPattern() {
		points = new ArrayList<JourneyPatternPoint>();
	}

}
