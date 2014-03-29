package nl.ovapi.rid.model;

import lombok.Getter;

public class StopPoint {
	
	public static class Builder{
		private Float latitude;
		private Float longitude;

		public Builder setLatitude(Float latitude){
			this.latitude = latitude;
			return this;
		}
		
		public Builder setLongitude(Float longitude){
			this.longitude = longitude;
			return this;
		}
		
		public StopPoint Build(){
			return new StopPoint(latitude,longitude);
		}
	}
	
	public static StopPoint.Builder newBuilder(){
		return new Builder();
	}
		
	public StopPoint(Float latitude, Float longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	@Getter private final Float latitude;
	@Getter private final Float longitude;
}
