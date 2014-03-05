package nl.ovapi.rid.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.ovapi.bison.model.KV17cvlinfo;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation.MutationType;
import nl.ovapi.bison.model.KV6posinfo;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.TooOldException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiStopTimeUpdate;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiTripDescriptor;
@ToString()
public class Journey {
	@Getter
	@Setter
	/**
	 * Trip_id of this journey
	 */
	private String id;
	@Getter
	@Setter
	/**
	 * DataOwnerCode:LinePlanningNumber:Journeynumber of trip, matches with KV6/17
	 */
	private String privateCode;
	@Getter
	@Setter
	/**
	 * JourneyPattern of the Journey
	 */
	private JourneyPattern journeypattern;
	@Getter
	@Setter
	/**
	 * TimeDemandGroup of the Journey
	 */
	private TimeDemandGroup timedemandgroup;

	@Getter
	@Setter
	/**
	 * Departuretime of the Journey, in seconds since midnight of operatingday (00:00:00).
	 */
	private Integer departuretime;
	@Getter
	@Setter
	/**
	 * Indicates whether Journey is accessible to Journey
	 */
	private Boolean wheelchairaccessible;
	@Getter
	@Setter
	/**
	 * agency_id of journey eg. (HTMBUZZ, BRENG, CXX).
	 * Is not equal to DataOwnerCode!.
	 */
	private String agencyId;
	@Getter
	@Setter
	/**
	 * ISO-8601 formatted (YYYY-MM-DD) OperatingDay.
	 */
	private String operatingDay;

	@Getter
	@Setter
	/**
	 * Indicates whether the trip is canceled
	 */
	private boolean isCanceled;

	@Getter
	@Setter
	private boolean isAdded;

	private static final Logger _log = LoggerFactory.getLogger(Journey.class);

	/**
	 * @return GTFS-Realtime TripDescriptor for this Journey
	 */
	public TripDescriptor.Builder tripDescriptor(){
		TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();
		tripDescriptor.setStartDate(operatingDay.replace("-", ""));
		tripDescriptor.setTripId(id.toString());
		tripDescriptor.setScheduleRelationship(isAdded ? ScheduleRelationship.ADDED : ScheduleRelationship.SCHEDULED);
		if (isCanceled)
			tripDescriptor.setScheduleRelationship(ScheduleRelationship.CANCELED);
		OVapiTripDescriptor.Builder extension = OVapiTripDescriptor.newBuilder();
		extension.setRealtimeTripId(privateCode);
		tripDescriptor.setExtension(GtfsRealtimeOVapi.ovapiTripdescriptor, extension.build());
		return tripDescriptor;
	}

	/**
	 * @return POSIX time when journey end in seconds since January 1st 1970 00:00:00 UTC
	 */

	public long getEndEpoch(){
		try {
			Calendar c = Calendar.getInstance(TimeZone.getDefault());
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(getOperatingDay()));
			c.set(Calendar.HOUR, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.SECOND, getDeparturetime());
			c.add(Calendar.SECOND, timedemandgroup.getPoints().get(timedemandgroup.getPoints().size()-1).getTotaldrivetime());
			return c.getTimeInMillis()/1000;
		} catch (ParseException e) {
			return -1;
		}
	}

	/**
	 * @return POSIX time when journey is scheduled to start in seconds since January 1st 1970 00:00:00 UTC
	 */

	public long getDepartureEpoch(){
		try {
			Calendar c = Calendar.getInstance(TimeZone.getDefault());
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(getOperatingDay()));
			c.set(Calendar.HOUR, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.SECOND, getDeparturetime());
			return c.getTimeInMillis()/1000;
		} catch (ParseException e) {
			return -1;
		}
	}

	public long getDepartureTime(int pointorder){
		for (TimeDemandGroupPoint tpt : getTimedemandgroup().getPoints()){
			if (tpt.getPointorder().equals(pointorder)){
				return getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
			}
		}
		throw new IllegalArgumentException("Pointorder "+pointorder+"does not exist");
	}

	public long getArrivalTime(int pointorder){
		for (TimeDemandGroupPoint tpt : getTimedemandgroup().getPoints()){
			if (tpt.getPointorder().equals(pointorder)){
				return getDepartureEpoch()+tpt.getTotaldrivetime();
			}
		}
		throw new IllegalArgumentException("Pointorder "+pointorder+"does not exist");
	}


	public JourneyPatternPoint getJourneyStop (String userstopcode,int passageSequencenumber){
		for (int i = 0; i < timedemandgroup.getPoints().size(); i++) {
			TimeDemandGroupPoint tpt = timedemandgroup.getPoints().get(i);
			JourneyPatternPoint pt = journeypattern.getPoint(tpt.pointorder);
			if (pt.getOperatorpointref().equals(userstopcode)){
				if (passageSequencenumber > 0){
					passageSequencenumber--;
				}else{
					return pt;
				}
			}
		}
		return null;
	}
}

