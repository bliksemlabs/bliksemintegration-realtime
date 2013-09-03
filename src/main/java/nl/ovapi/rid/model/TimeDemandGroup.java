package nl.ovapi.rid.model;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
public class TimeDemandGroup {

	public static class TimeDemandGroupPoint {
		@Getter
		@Setter
		public Integer pointorder;
		@Getter
		@Setter
		public Integer totaldrivetime;
		@Getter
		@Setter
		public Integer stopwaittime;
	}

	@Getter private ArrayList<TimeDemandGroupPoint> points;

	public void add(TimeDemandGroupPoint point){
		points.add(point);
	}
	
	public TimeDemandGroup() {
		points = new ArrayList<TimeDemandGroupPoint>();
	}
}
