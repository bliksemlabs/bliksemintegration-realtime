package nl.ovapi.rid.model;

import lombok.Getter;

public class StopPoint {
	
	public static class Builder{
		private float latitude;
		private float longitude;

		private Builder(){}
		
		/**
		 * 
		 * @param latitude of stoppoint
		 * @return builder instance
		 */
		public Builder setLatitude(Float latitude){
			this.latitude = latitude;
			return this;
		}
		
		/**
		 * 
		 * @param longitude of stoppoint
		 * @return builder instance
		 */
		public Builder setLongitude(Float longitude){
			this.longitude = longitude;
			return this;
		}
		/**
		 * @return StopPoint instance with content set in builder
		 */
		public StopPoint build(){
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
