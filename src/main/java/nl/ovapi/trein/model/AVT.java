package nl.ovapi.trein.model;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.ovapi.bison.DateUtils;

@ToString()
public class AVT {

	//\LVervoerder|RitNummer|VertrekTijd|VertrekVertraging|EindBestemming|TreinSoort|RouteTekst|
	//VertrekSpoor|SpoorWijziging|ReisTip|Opmerkingen

	@Getter @Setter private String operator;
	@Getter @Setter private Integer journeynumber;
	@Getter @Setter private Long departuretime;
	@Getter @Setter private Integer punctuality;
	@Getter @Setter private String destination;
	@Getter @Setter private String productCategory;
	@Getter @Setter private String routeText;
	@Getter @Setter private String departurePlatform;
	@Getter @Setter private boolean platformChanged;
	@Getter @Setter private String advice;
	@Getter @Setter private String remarks;

	public static AVT fromCtxLine(String line){
		AVT avt = new AVT();
		String[] v = line.split("\\|");
		if (v[0].length() > 0)
			avt.setOperator(v[0]);
		if (v[1].length() > 0)
			avt.setJourneynumber(Integer.valueOf(v[1]));
		avt.setDeparturetime(DateUtils.parse(v[2]));
		avt.setPunctuality(Integer.valueOf(v[3]));
		avt.setDestination(v[4]);
		avt.setProductCategory(v[5]);
		avt.setRouteText(v[6]);
		avt.setDeparturePlatform(v[7]);
		avt.setPlatformChanged(Boolean.valueOf(v[8]));
		if (v.length >= 10)
			avt.setAdvice(v[9]);
		if (v.length >= 11)
			avt.setRemarks(v[10]);
		return avt;
	}

	public static ArrayList<AVT> fromCtx(String ctx){
		ArrayList<AVT> result = new ArrayList<AVT>();
		for (String line : ctx.split("\r\n")){
			if (line.charAt(0) == '\\'){
				continue;
			}
			result.add(fromCtxLine(line));
		}
		return result;
	}
}
