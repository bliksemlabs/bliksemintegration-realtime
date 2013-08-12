package nl.ovapi.bison.sax;

import nl.ovapi.bison.model.KV15message;
import nl.ovapi.bison.model.SubEffectType;

import com.google.transit.realtime.GtfsRealtime.Alert.Cause;
import com.google.transit.realtime.GtfsRealtime.Alert.Effect;

public class BisonToGtfsUtils {

	public static String text(KV15message msg){
		StringBuilder sb = new StringBuilder();
		if (msg.getSubReasonType() != null || (msg.getReasonContent() != null && msg.getReasonContent().equals(msg.getMessageContent()))){
			sb.append("Oorzaak : ");
			if (msg.getSubReasonType() != null){
				sb.append(msg.getSubReasonType());
				sb.append(" ");
			}
			if (msg.getReasonContent() != null && msg.getReasonContent().equals(msg.getMessageContent())){
				sb.append(msg.getReasonContent());
			}
			sb.append("\n");
		}
		if ((msg.getSubEffectType() != null && msg.getSubEffectType() != SubEffectType.UNKNOWN)
				|| (msg.getEffectContent() != null && msg.getEffectContent().equals(msg.getMessageContent()))){
			sb.append("Effect : ");
			if (msg.getSubEffectType() != null && msg.getSubEffectType() != SubEffectType.UNKNOWN){
				sb.append(msg.getSubEffectType());
				sb.append(" ");
			}
			if (msg.getEffectContent() != null && msg.getEffectContent().equals(msg.getMessageContent())){
				sb.append(msg.getEffectContent());
			}
			sb.append("\n");
		}
		if (msg.getSubMeasureType() != null || msg.getMeasureContent() != null){
			sb.append("Maatregelen : ");
			if (msg.getSubMeasureType() != null){
				sb.append(msg.getSubMeasureType());
				sb.append(" ");
			}
			if (msg.getMeasureContent() != null){
				sb.append(msg.getMeasureContent());
			}
			sb.append("\n");
		}
		if (msg.getMessageContent() != null){
			sb.append(msg.getMessageContent());
			sb.append("\n");
		}
		return sb.toString();
	}

	public static Cause getCause(KV15message msg){
		if (msg.getSubReasonType() == null && msg.getReasonType() != null){
			switch (msg.getReasonType()){
			case GENERAL:
				return Cause.OTHER_CAUSE;
			case UNDEF:
			case UNKNOWN:
				return Cause.UNKNOWN_CAUSE;	
			}
		}else if (msg.getSubReasonType() != null){
			switch (msg.getSubReasonType()){
			case Aanrijding:
			case Aanrijding_met_Persoon:
			case Route_versperd:
			case Brand:
			case Ongeval:
			case Ontsporing:
			case Auto_in_spoor:
				return Cause.ACCIDENT;
			case Asfalteringswerkzaamheden:
			case Bestratingswerkzaamheden:
			case Rioleringswerkzaamheden:
			case Uitloop_herstel_werkzaamheden:
			case Uitloop_werkzaamheden:
			case Wegwerkzaamheden:
			case Werkzaamheden:
				return Cause.MAINTENANCE;
			case Blikseminslag:
			case IJsgang:
			case IJzel:
			case Gladde_sporen:
			case Sneeuw:
			case Storm:
			case Gladheid:
			case Omgevallen_bomen:
				return Cause.WEATHER;
			case Bommelding:
			case Mensen_op_de_route:
			case Last_van_de_Politie:
			case Ontruiming:
				return Cause.POLICE_ACTIVITY;
			case Braderie:
			case Bloemencorso:
			case Carnaval:
			case Marathon:
			case Herdenking:
			case Avondvierdaagse:
			case Jaarmarkt:
			case Wielerronde:
			case Voetbalwedstrijd:
			case Kermis:
			case Optocht:
			case Wateroverlast:
			case Koninginnedag:
				return Cause.HOLIDAY;
			case Defect_materieel:
			case Defect_spoor:
			case Sein_en_wisselstoring:
			case Defect_viaduct:
			case Defecte_bovenleiding:
			case Defecte_trein:
			case Seinstoring:
			case Defecte_brug:
			case Wisselstoring:
			case Storing_in_verkeersleidingssysteem:
			case Overwegstoring:
				return Cause.TECHNICAL_PROBLEM;
			case Eerdere_verstoring:
			case Extreme_drukte:
			case File:
			case Herstel_werkzaamheden:
			case Last_van_de_Brandweer:
			case Logistieke_problemen:
			case Tekort_aan_materieel:
			case Tekort_aan_personeel:
			case Tweede_wereldoorlog_bom:
			case Stroomstoring:
			case Stremming:
			case Snelheidsbeperkingen:
			case Vee_op_de_route:
				return Cause.OTHER_CAUSE;
			case Staking:
			case Stiptheidsacties:
			case Vakbondsacties:
			case Mogelijke_staking:
				return Cause.STRIKE;
			case Passagier_onwel:
				return Cause.MEDICAL_EMERGENCY;
			default:
			case NULL:
			case Onbekend:
				return Cause.UNKNOWN_CAUSE;
			}
		}

		return Cause.UNKNOWN_CAUSE;
	}

	public static Effect getEffect(KV15message msg){
		if (msg.getSubMeasureType() != null){
			switch (msg.getSubMeasureType()){
			case BUS:
				return Effect.MODIFIED_SERVICE;
			case CANCELLED_STOPS:
				return Effect.DETOUR;
			case DIVERSION:
			case DIVERTED_TRAIN:
				return Effect.DETOUR;
			case EXTRA_TRANSPORT:
				return Effect.ADDITIONAL_SERVICE;
			case LIMITED_BUS:
			case LIMITED_TRAIN:
				return Effect.REDUCED_SERVICE;
			case NONE:
				return Effect.OTHER_EFFECT;
			case NO_BUS:
			case NO_TRAIN:
				return Effect.NO_SERVICE;
			case ROUTEMODIFIED:
				return Effect.DETOUR;
			case SPECIAL_STOP:
				return Effect.MODIFIED_SERVICE;
			case UNKNOWN:
				return Effect.UNKNOWN_EFFECT;

			}
		}
		if (msg.getSubEffectType() != null){
			switch (msg.getSubEffectType()){
			case DECREASED_SERVICE:
				return Effect.REDUCED_SERVICE;
			case DELAYED_DIVERSION:
			case DELAY_10:
			case DELAY_5:
			case DELAY_1015:
			case DELAY_15:
			case DELAY_1530:
			case DELAY_30:
			case DELAY_3060:
			case DELAY_45:
			case DELAY_510:
			case DELAY_60:
			case DELAY_60PLUS:
			case DELAY_UNKNOWN:
				return Effect.SIGNIFICANT_DELAYS;
			case DISRUPTED:
				return Effect.MODIFIED_SERVICE;
			case DIVERSION:
				return Effect.DETOUR;
			case LINECANCEL:
			case NO_SERVICE:
			case NO_TRAINS:
			case STOPCANCEL:
				return Effect.NO_SERVICE;
			case UNKNOWN:
				return Effect.UNKNOWN_EFFECT;
			}
		}
		return Effect.UNKNOWN_EFFECT;
	}

}
