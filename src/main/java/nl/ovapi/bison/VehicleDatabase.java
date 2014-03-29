package nl.ovapi.bison;

import lombok.Getter;
import lombok.Setter;
import nl.ovapi.bison.model.DataOwnerCode;
import nl.ovapi.bison.model.KV6posinfo;

public class VehicleDatabase {
	public static class VehicleType{
		public VehicleType(String vehicle_type, boolean wheelchair_accessible){
			this.vehicleType = vehicle_type;
			this.wheelchairAccessible = wheelchair_accessible;
		}
		@Getter @Setter private String vehicleType;
		@Getter @Setter private boolean wheelchairAccessible;
	}

	public static VehicleType vehicleType(KV6posinfo posinfo){
		if (posinfo.getVehiclenumber() == null){
			return null;
		}
		if (DataOwnerCode.HTM == posinfo.getDataownercode()){
			if (posinfo.getVehiclenumber() != null){
				if ((posinfo.getVehiclenumber() >= 3001 && posinfo.getVehiclenumber() <= 3099) ||
						(posinfo.getVehiclenumber() >= 3100 && posinfo.getVehiclenumber() <= 3147)){
					return new VehicleType("GTL8",false);
				}
				else if (posinfo.getVehiclenumber() >= 4001 && posinfo.getVehiclenumber() <= 4072){
					return new VehicleType("RegioCitadis",true);
				}else if (posinfo.getVehiclenumber() >= 1001 && posinfo.getVehiclenumber() <= 1135){
					return new VehicleType("MAN Lion's City (CNG)",true);
				}
			}
			return null;
		}else if (DataOwnerCode.RET == posinfo.getDataownercode()){
			if (posinfo.getVehiclenumber() != null){
				if (posinfo.getVehiclenumber() >= 702 && posinfo.getVehiclenumber() <= 750){
					return new VehicleType("ZGT",false);
				}else if (posinfo.getVehiclenumber() >= 2001 && posinfo.getVehiclenumber() <= 2060){
					return new VehicleType("Citadis",true);
				}else if (posinfo.getVehiclenumber() >= 2101 && posinfo.getVehiclenumber() <= 2153){
					return new VehicleType("Citadis2",true);
				}else if (posinfo.getVehiclenumber() >= 1001 && posinfo.getVehiclenumber() <= 1090){
					return new VehicleType("MAN Lion's City TÜ",true);
				}else if ((posinfo.getVehiclenumber() >= 201 && posinfo.getVehiclenumber() <= 290) ||
						(posinfo.getVehiclenumber() >= 301 && posinfo.getVehiclenumber() <= 376)){
					return new VehicleType("Mercedes-Benz Citaro",true);
				}else if (posinfo.getVehiclenumber() >= 401 && posinfo.getVehiclenumber() <= 402){
					return new VehicleType("Mercedes-Benz Citaro Hybride",true);
				}
			}
			return null;
		}else if (DataOwnerCode.QBUZZ == posinfo.getDataownercode()){
			if (posinfo.getVehiclenumber() >= 2001 && posinfo.getVehiclenumber() <= 2041){
				return new VehicleType("MAN Lion's City TÜ",true);
			}else if (posinfo.getVehiclenumber() >= 2050 && posinfo.getVehiclenumber() <= 2056){
				return new VehicleType("MAN Lion's City Ü CNG",true);
			}else if (posinfo.getVehiclenumber() >= 3001 && posinfo.getVehiclenumber() <= 3025){
				return new VehicleType("Mercedes-Benz Citaro",true);
			}else if (posinfo.getVehiclenumber() >= 3050 && posinfo.getVehiclenumber() <= 3079){
				return new VehicleType("Mercedes-Benz Citaro G",true);
			}else if (posinfo.getVehiclenumber() >= 3090 && posinfo.getVehiclenumber() <= 3097){
				return new VehicleType("Mercedes-Benz Citaro G P+R Citybus",true);
			}else if (posinfo.getVehiclenumber() >= 3100 && posinfo.getVehiclenumber() <= 3326){
				return new VehicleType("Mercedes-Benz Citaro Streek",true);
			}else if (posinfo.getVehiclenumber() >= 3500 && posinfo.getVehiclenumber() <= 3523){
				return new VehicleType("Mercedes-Benz Citaro G",true);
			}else if (posinfo.getVehiclenumber() >= 3600 && posinfo.getVehiclenumber() <= 3640){
				return new VehicleType("Mercedes-Benz Integro",true);
			}else if (posinfo.getVehiclenumber() >= 3800 && posinfo.getVehiclenumber() <= 3802){
				return new VehicleType("Mercedes-Benz Sprinter",false);
			}
			return null;
		}else if (DataOwnerCode.ARR == posinfo.getDataownercode()){
			if (posinfo.getVehiclenumber() >= 6451 && posinfo.getVehiclenumber() <= 6464){
				return new VehicleType("Irisbus Crossway LE",true);
			}else if (posinfo.getVehiclenumber() == 6522){
				return new VehicleType("Mercedes-Benz Sprinter",false);
			}
		}
		return null;
	}
}
