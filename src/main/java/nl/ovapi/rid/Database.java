package nl.ovapi.rid;

public class Database {

	public final static String journeyQuery = 
			"SELECT validdate||':'||journey.privatecode as key,journey.id,journeypatternref,timedemandgroupref,departuretime,(lowfloor or hasliftorramp) as wheelchairaccessible,o.privatecode as operatorcode,validdate::text,journey.privatecode,lineref,availabilityconditionref \n" +
					"FROM journey JOIN availabilityconditionday USING (availabilityconditionref) JOIN journeypattern as j ON (j.id = journeypatternref) JOIN route as r ON (r.id = routeref) JOIN line as l ON (l.id = lineref) JOIN operator as o ON (operatorref = o.id) "+
					"WHERE isavailable = true AND validdate in (date 'yesterday',date 'today',date 'tomorrow') AND coalesce(monitored,true) = true AND journey.privatecode not like 'IFF:%';";

	public final static String trainQuery = 
			"SELECT validdate||':'||journey.privatecode as key,journey.id,journeypatternref,timedemandgroupref,departuretime,(lowfloor or hasliftorramp) as wheelchairaccessible,o.privatecode as operatorcode,validdate::text,journey.privatecode,lineref,blockref \n"+
			"FROM journey JOIN availabilityconditionday USING (availabilityconditionref) JOIN journeypattern as j ON (j.id = journeypatternref) JOIN route as r ON (r.id = routeref) JOIN line as l ON (l.id = lineref) JOIN operator as o ON (operatorref = o.id)\n"+
			"WHERE isavailable = true AND validdate in (date 'yesterday',date 'today',date 'tomorrow') AND coalesce(monitored,true) = true AND journey.privatecode like 'IFF:%'\n"+
			"ORDER BY validdate,blockref,departuretime;";
	
	public final static String journeyPatternQuery = 
			"SELECT journeypatternref,pointorder,pointref,s.privatecode as operatorpointref,iswaitpoint,distancefromstartroute,isscheduled,split_part(coalesce(d.operator_id,dj.operator_id),':',2) as destinationcode,platformcode,directiontype,forboarding,foralighting FROM pointinjourneypattern JOIN stoppoint as s ON (s.id = pointref) LEFT JOIN destinationdisplay as d ON (d.id = destinationdisplayref) JOIN journeypattern as jp ON (jp.id = journeypatternref) LEFT JOIN destinationdisplay as dj ON (dj.id = jp.destinationdisplayref)"+
					"WHERE journeypatternref in (SELECT DISTINCT journeypatternref FROM journey JOIN availabilityconditionday USING (availabilityconditionref) JOIN journeypattern as j ON (j.id = journeypatternref) JOIN route as r ON (r.id = routeref) JOIN line as l ON (l.id = lineref) WHERE isavailable = true AND validdate in (date 'yesterday',date 'today',date 'tomorrow') AND coalesce(monitored,true) = true) "+
					"ORDER BY journeypatternref,pointorder;";

	public final static String timepatternQuery =
			"SELECT timedemandgroupref,pointorder,totaldrivetime,stopwaittime FROM pointintimedemandgroup "+
					"WHERE timedemandgroupref in (SELECT DISTINCT timedemandgroupref FROM journey JOIN availabilityconditionday USING (availabilityconditionref) LEFT JOIN journeypattern as j ON (j.id = journeypatternref) LEFT JOIN route as r ON (r.id = routeref) LEFT JOIN line as l ON (l.id = lineref) WHERE isavailable = true AND validdate in (date 'yesterday',date 'today',date 'tomorrow') AND coalesce(monitored,true) = true) "+
					"ORDER BY timedemandgroupref,pointorder;";

	public final static String stoppointQuery = "SELECT id,latitude,longitude,operator_id FROM stoppoint";
	
	public final static String lineQuery = "SELECT id,operator_id FROM line";
	
	public final static String gvbJourneyQuery = "select concat_ws(':',validdate,olddataownercode,oldlineplanningnumber,oldjourneynumber) as oldprivatecode,concat_ws(':',validdate,dataownercode,lineplanningnumber,journeynumber) as privatecode \n"+
                  "from gvb_journeynumber_mapping where validdate in (date 'yesterday', date 'today', date 'tomorrow') AND journeynumber != oldjourneynumber;;";


	public final static String kv15Query = "SELECT dataownercode,messagecodedate,messagecodenumber,userstopcodes,messagepriority,messagetype,messagedurationtype,messagestarttime,messageendtime,messagecontent,reasontype,subreasontype,reasoncontent,effecttype,subeffecttype,effectcontent,advicetype,subadvicetype,advicecontent,messagetimestamp,measuretype,submeasuretype,measurecontent,lineplanningnumbers "+
			"FROM kv15_stopmessage LEFT JOIN (SELECT dataownercode,messagecodedate,messagecodenumber,string_agg(userstopcode,';') as userstopcodes "+
			"        FROM  kv15_stopmessage_userstopcode GROUP BY dataownercode,messagecodedate,messagecodenumber) as u USING (dataownercode,messagecodedate,messagecodenumber) "+
			" LEFT JOIN (SELECT dataownercode,messagecodedate,messagecodenumber,string_agg(lineplanningnumber,';') as lineplanningnumbers "+
			"        FROM  kv15_stopmessage_lineplanningnumber GROUP BY dataownercode,messagecodedate,messagecodenumber) as l USING (dataownercode,messagecodedate,messagecodenumber) "+
			"WHERE ((messagetype != 'REMOVE' AND messageendtime is null) OR messageendtime > date 'now') and coalesce(isdeleted,false) = false AND (dataownercode = 'QBUZZ' OR messagepriority != 'COMMERCIAL')";
}
