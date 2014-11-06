package nl.ovapi.rid.model;

import lombok.Getter;
import lombok.ToString;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiTripDescriptor;
@ToString()
public class Journey {
	private static final Logger _log = LoggerFactory.getLogger(Journey.class);

	public static class Builder{
		@Getter
		private String id;

		/**
		 * Set trip_id of this journey
		 */
		public Builder setId(String id){
			this.id = id;
			return this;
		}

		@Getter
		private String privateCode;

		/**
		 * Set DataOwnerCode:LinePlanningNumber:Journeynumber of trip, matches with KV6/17
		 */
		public Builder setPrivateCode(String privateCode){
			this.privateCode = privateCode;
			return this;
		}

		@Getter
		private JourneyPattern journeypattern;
		/**
		 * Set JourneyPattern of the Journey
		 */
		public Builder setJourneyPattern(JourneyPattern journeypattern){
			this.journeypattern = journeypattern;
			return this;
		}


		@Getter
		private TimeDemandGroup timedemandgroup;

		/**
		 * Set TimeDemandGroup of the Journey
		 */
		public Builder setTimeDemandGroup(TimeDemandGroup timedemandgroup){
			this.timedemandgroup = timedemandgroup;
			return this;
		}


		@Getter
		/**
		 * Departuretime of the Journey, in seconds since midnight of operatingday (00:00:00).
		 */
		private Integer departuretime;

		/**
		 * Set TimeDemandGroup of the Journey
		 */
		public Builder setDeparturetime(Integer departuretime){
			this.departuretime = departuretime;
			return this;
		}

		@Getter
		private Boolean wheelchairaccessible;
		/**
		 * Set whether Journey is accessible to Journey
		 */
		public Builder setWheelchairaccessible(Boolean wheelchairaccessible){
			this.wheelchairaccessible = wheelchairaccessible;
			return this;
		}

		@Getter
		private String agencyId;
		/**
		 * Set agency_id of journey eg. (HTMBUZZ, BRENG, CXX).
		 * Is not equal to DataOwnerCode!.
		 */
		public Builder setAgencyId(String agencyId){
			this.agencyId = agencyId;
			return this;
		}

		@Getter
		private LocalDate operatingDay;
		/**
		 * Set ISO-8601 formatted (YYYY-MM-DD) OperatingDay.
		 */
		public Builder setOperatingDay(String operatingDay){
			this.operatingDay = LocalDate.parse(operatingDay);
			return this;
		}


		@Getter
		private Long routeId;
		/**
		 * Set route_id of this journey.
		 */
		public Builder setRouteId(Long routeId){
			this.routeId = routeId;
			return this;
		}

		@Getter
		private Long availabilityConditionRef;
		/**
		 * Set service_id / availabilityconditionref of this journey.
		 */
		public Builder setAvailabilityConditionRef(Long availabilityConditionRef){
			this.availabilityConditionRef = availabilityConditionRef;
			return this;
		}

		@Getter
		private boolean isAdded = false;
		/**
		 * Set whether journey is added on top on scheduled.
		 */
		public Builder setIsAdded(boolean isAdded){
			this.isAdded = isAdded;
			return this;
		}

		@Getter
		private boolean isCanceled = false;
		/**
		 * Set whether journey is added on top on scheduled.
		 */
		public Builder setIsCanceled(boolean isCanceled){
			this.isCanceled = isCanceled;
			return this;
		}

		@Getter
		private String blockRef;
		/**
		 * Set block_id / blockRef of journey.
		 */
		public Builder setBlockRef(String blockRef){
			this.blockRef = blockRef;
			return this;
		}

		public Builder(){}

		public Builder(Journey journey){
			this.id = journey.id;
			this.privateCode = journey.privateCode;
			this.journeypattern = journey.journeypattern;
			this.timedemandgroup = journey.timedemandgroup;
			this.departuretime = journey.departuretime;
			this.wheelchairaccessible = journey.wheelchairaccessible;
			this.agencyId = journey.agencyId;
			this.operatingDay = journey.operatingDay;
			this.routeId = journey.routeId;
			this.availabilityConditionRef= journey.availabilityConditionRef;
			this.isAdded = journey.isAdded;
			this.isCanceled = journey.isCanceled;
			this.blockRef = journey.blockRef;
		}

		public Journey build(){
			if (operatingDay == null){
				throw new IllegalArgumentException("Invalid operatingday "+operatingDay);
			}
			if (departuretime == null){
				throw new IllegalArgumentException("Depaturetime required");
			}
			return new Journey(id,privateCode,journeypattern,timedemandgroup,departuretime,
					wheelchairaccessible,agencyId,operatingDay,routeId,availabilityConditionRef,
					isAdded,isCanceled,blockRef);
		}

	}

	@Getter
	/**
	 * Trip_id of this journey
	 */
	private final String id;
	@Getter
	/**
	 * DataOwnerCode:LinePlanningNumber:Journeynumber of trip, matches with KV6/17
	 */
	private final String privateCode;
	@Getter
	/**
	 * JourneyPattern of the Journey
	 */
	private final JourneyPattern journeypattern;
	@Getter
	/**
	 * TimeDemandGroup of the Journey
	 */
	private final TimeDemandGroup timedemandgroup;

	@Getter
	/**
	 * Departuretime of the Journey, in seconds since midnight of operatingday (00:00:00).
	 */
	private final int departuretime;
	@Getter
	/**
	 * Indicates whether Journey is accessible to Journey
	 */
	private final Boolean wheelchairaccessible;
	@Getter
	/**
	 * agency_id of journey eg. (HTMBUZZ, BRENG, CXX).
	 * Is not equal to DataOwnerCode!.
	 */
	private final String agencyId;
	@Getter
	/**
	 * ISO-8601 formatted (YYYY-MM-DD) OperatingDay.
	 */
	private final LocalDate operatingDay;

	@Getter
	/**
	 * ID of route.
	 */
	private final Long routeId;

	@Getter
	/**
	 * Availabilitycondition.
	 */
	private final Long availabilityConditionRef;

	@Getter
	private final boolean isAdded;

	@Getter
	private final boolean isCanceled;

	@Getter
	private final String blockRef;

	public Journey(String id, String privateCode,
			JourneyPattern journeypattern, TimeDemandGroup timedemandgroup,
			Integer departuretime, Boolean wheelchairaccessible,
			String agencyId, LocalDate operatingDay, Long routeId,
			Long availabilityConditionRef, boolean isAdded,
			boolean isCanceled, String blockRef) {
		this.id = id;
		this.privateCode = privateCode;
		this.journeypattern = journeypattern;
		this.timedemandgroup = timedemandgroup;
		this.departuretime = departuretime;
		this.wheelchairaccessible = wheelchairaccessible;
		this.agencyId = agencyId;
		this.operatingDay = operatingDay;
		this.routeId = routeId;
		this.availabilityConditionRef= availabilityConditionRef;
		this.isAdded = isAdded;
		this.isCanceled = isCanceled;
		this.blockRef = blockRef;
	}

	private final static DateTimeFormatter GTFSRT_SERVICEDAY_FORMAT = DateTimeFormat.forPattern("yyyyMMdd");

	/**
	 * @return GTFS-Realtime TripDescriptor for this Journey
	 */
	public TripDescriptor.Builder tripDescriptor(){
		TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();
		tripDescriptor.setStartDate(operatingDay.toString(GTFSRT_SERVICEDAY_FORMAT));
		tripDescriptor.setTripId(id);
		if (isAdded){
			if (routeId != null){
				tripDescriptor.setRouteId(routeId+"");
			}
			tripDescriptor.setScheduleRelationship(ScheduleRelationship.ADDED);
		}else if (isCanceled){
			tripDescriptor.setScheduleRelationship(ScheduleRelationship.CANCELED);
		}else{
			tripDescriptor.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
		}
		OVapiTripDescriptor.Builder extension = OVapiTripDescriptor.newBuilder();
		extension.setRealtimeTripId(privateCode);
		tripDescriptor.setExtension(GtfsRealtimeOVapi.ovapiTripdescriptor, extension.build());
		return tripDescriptor;
	}

	private final static DateTimeZone TIMEZONE = DateTimeZone.forID("Europe/Amsterdam");
	/**
	 * @return POSIX time when journey end in seconds since January 1st 1970 00:00:00 UTC
	 */

	public long getEndEpoch(){
		int totalDriveTime = timedemandgroup.getPoints().get(timedemandgroup.getPoints().size()-1).getTotaldrivetime();
		DateTime dtEnd = RIDservice.toDateTime(getOperatingDay(),TIMEZONE,getDeparturetime(),totalDriveTime);
		return dtEnd != null ? dtEnd.getMillis() / 1000 : -1;
	}

	/**
	 * @return POSIX time when journey is scheduled to start in seconds since January 1st 1970 00:00:00 UTC
	 */

	public long getDepartureEpoch(){
		DateTime dtStart = RIDservice.toDateTime(getOperatingDay(),TIMEZONE,getDeparturetime(),0);
		return dtStart != null ? dtStart.getMillis() / 1000 : -1;
	}

	public long getDepartureTime(int pointorder){
		for (TimeDemandGroupPoint tpt : getTimedemandgroup().getPoints()){
			if (tpt.getPointorder() == pointorder){
				return getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
			}
		}
		throw new IllegalArgumentException("Pointorder "+pointorder+"does not exist");
	}

	public DateTime getDepartureDateTime(int pointorder){
		for (TimeDemandGroupPoint tpt : getTimedemandgroup().getPoints()){
			if (tpt.getPointorder() == pointorder){
				return RIDservice.toDateTime(getOperatingDay(),TIMEZONE,getDeparturetime(),tpt.getTotaldrivetime()+tpt.getStopwaittime());
			}
		}
		throw new IllegalArgumentException("Pointorder "+pointorder+"does not exist");
	}

	public DateTime getArrivalDateTime(int pointorder){
		for (TimeDemandGroupPoint tpt : getTimedemandgroup().getPoints()){
			if (tpt.getPointorder() == pointorder){
				return RIDservice.toDateTime(getOperatingDay(),TIMEZONE,getDeparturetime(),tpt.getTotaldrivetime());
			}
		}
		throw new IllegalArgumentException("Pointorder "+pointorder+"does not exist");
	}

	public long getArrivalTime(int pointorder){
		for (TimeDemandGroupPoint tpt : getTimedemandgroup().getPoints()){
			if (tpt.getPointorder() == pointorder){
				return getDepartureEpoch()+tpt.getTotaldrivetime();
			}
		}
		throw new IllegalArgumentException("Pointorder "+pointorder+"does not exist");
	}

	public JourneyPatternPoint getJourneyStop (String userstopcode,int passageSequencenumber){
		for (int i = 0; i < timedemandgroup.getPoints().size(); i++) {
			TimeDemandGroupPoint tpt = timedemandgroup.getPoints().get(i);
			JourneyPatternPoint pt = journeypattern.getPoint(tpt.getPointorder());
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

	public static Builder newBuilder(){
		return new Builder();
	}

	public Builder edit(){
		return new Builder(this);
	}
}

