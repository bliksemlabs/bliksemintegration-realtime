package nl.ovapi.bison.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.ovapi.bison.VehicleDatabase;
import nl.ovapi.bison.VehicleDatabase.VehicleType;

import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiVehicleDescriptor;

@ToString()
public class KV6posinfo {
	public enum Type {
		DELAY, INIT, ARRIVAL, DEPARTURE, ONSTOP, ONROUTE, OFFROUTE, END
	}

	@Getter
	@Setter
	private Type messagetype;
	@Getter
	@Setter
	private DataOwnerCode dataownercode;
	@Getter
	@Setter
	private String lineplanningnumber;
	@Getter
	@Setter
	private String operatingday;
	@Getter
	@Setter
	private Integer journeynumber;

	@Setter
	private Integer reinforcementnumber;
	public Integer getReinforcementnumber(){
		return reinforcementnumber == null ? 0 : reinforcementnumber;
	}
	
	@Getter
	@Setter
	private Long timestamp;
	@Getter
	@Setter
	private Source source;
	@Getter
	@Setter
	private String userstopcode;
	@Getter
	@Setter
	private Integer passagesequencenumber;
	@Getter
	@Setter
	private Integer vehiclenumber;
	@Getter
	@Setter
	private Integer blockcode;
	@Getter
	@Setter
	private WheelChairAccessible wheelchairaccessible;
	@Getter
	@Setter
	private Short numberofcoaches;
	@Getter
	@Setter
	private Integer distancesincelastuserstop;
	@Getter
	@Setter
	private Integer rd_x;
	@Getter
	@Setter
	private Integer rd_y;
	@Getter
	@Setter
	private Integer punctuality;
	
	public VehicleDescriptor.Builder getVehicleDescription(){
		if (vehiclenumber == null){
			return null;
		}
		VehicleDescriptor.Builder vehicleDesc = VehicleDescriptor.newBuilder();
		vehicleDesc.setId(String.format("%s:%s", getDataownercode().name(),getVehiclenumber().toString()));
		vehicleDesc.setLabel(getVehiclenumber().toString());
		VehicleType type = VehicleDatabase.vehicleType(this);
		if (type != null){
			OVapiVehicleDescriptor.Builder extension = OVapiVehicleDescriptor.newBuilder();
			extension.setVehicleType(type.getVehicleType());
			extension.setWheelchairAccessible(type.isWheelchairAccessible());
			vehicleDesc.setExtension(GtfsRealtimeOVapi.ovapiVehicleDescriptor, extension.build());
		}
		return vehicleDesc;
	}
}
