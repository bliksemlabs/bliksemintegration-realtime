package nl.ovapi.bison.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation;
import nl.ovapi.bison.model.KV17cvlinfo.Mutation.MutationType;
import nl.ovapi.bison.model.KV6posinfo.Type;
import nl.ovapi.exceptions.StopNotFoundException;
import nl.ovapi.exceptions.TooEarlyException;
import nl.ovapi.exceptions.TooOldException;
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.gtfsrt.Utils;
import nl.ovapi.rid.gtfsrt.services.GeometryService;
import nl.ovapi.rid.gtfsrt.services.RIDservice;
import nl.ovapi.rid.model.Journey;
import nl.ovapi.rid.model.StopPoint;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.Position.Builder;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtimeOVapi;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiStopTimeUpdate;
import com.google.transit.realtime.GtfsRealtimeOVapi.OVapiVehiclePosition;

public class JourneyProcessor {


	@Getter
	/**
	 * Last received KV6posinfo for this Journey
	 */
	private KV6posinfo posinfo;
	
	private Map<Integer, ArrayList<Mutation>> mutations;
	
	@Getter
	@Setter
	/**
	 * Map with last received posinfo for reinforcementnumber.
	 */
	private Map<Integer, KV6posinfo> reinforcements;
	private Map<Integer, Long> realizedArrivals;
	private Map<Integer, Long> realizedDepartures;
	
	private static final Logger _log = LoggerFactory.getLogger(JourneyProcessor.class);

	private Journey _journey;
	public JourneyProcessor(@NonNull Journey journey){
		_journey = journey;
		mutations = Maps.newHashMap();
		reinforcements = Maps.newHashMap();
		realizedArrivals = Maps.newHashMap();
		realizedDepartures = Maps.newHashMap();
	}
	

	public void clearKV6(){
		posinfo = null;
	}

	private static final boolean RECORD_TIMES = true;

	private static final int PUNCTUALITY_FLOOR = 15; // seconds
	private static final int DEFAULT_SPEED = (int) (75 / 3.6); // meters per
	// seconds
	private static final int LONGHAUL_SPEED = (int) (95 / 3.6); // meters per
	// seconds
	private static final int SHORTHAUL_SPEED = (int) (45 / 3.6); // meters per
	// seconds

	private static final int MIN_PUNCTUALITY = -300; // Minimum allowed
	// punctuality.

	private static final int MIN_STOPWAITTIME = -300; // Time it takes to unload a bus at a major stop eg a trainstation.
	// punctuality.


	
	public boolean hasMutations(){
		return mutations.size() > 0 || _journey.isCanceled();
	}
	

	public StopTimeEvent.Builder stopTimeEventDeparture(TimeDemandGroupPoint tpt,JourneyPatternPoint pt, int punctuality){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		if (mutations.containsKey(pt.getPointorder())){
			for (Mutation m : mutations.get(pt.getPointorder())){
				if (m.getMutationtype() == MutationType.LAG){
					punctuality = Math.max(punctuality, m.getLagtime());
				}
			}
		}
		stopTimeEvent.setDelay(punctuality);
		stopTimeEvent.setTime(_journey.getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime()+punctuality);
		return stopTimeEvent;
	}

	public StopTimeEvent.Builder stopTimeEventDepartureRecorded(TimeDemandGroupPoint tpt, long time){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		long targettime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
		int delay = (int)(time-targettime);
		if (Math.abs(delay) < PUNCTUALITY_FLOOR || delay <  MIN_PUNCTUALITY*2){
			stopTimeEvent.setDelay(0);
			stopTimeEvent.setTime(targettime);
		}else{
			stopTimeEvent.setDelay(delay);
			stopTimeEvent.setTime(time);
		}
		return stopTimeEvent;
	}
	

	private StopTimeEvent.Builder stopTimeEventArrivalRecorded(TimeDemandGroupPoint tpt, long time){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		long targettime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime();
		int delay = (int)(time-targettime);
		if (Math.abs(delay) < PUNCTUALITY_FLOOR || delay <  MIN_PUNCTUALITY*2){
			stopTimeEvent.setDelay(0);
			stopTimeEvent.setTime(targettime);
		}else{
			stopTimeEvent.setDelay(delay);
			stopTimeEvent.setTime(time);
		}
		return stopTimeEvent;
	}
	
	/**
	 * @return POSIX time when journey end in seconds since January 1st 1970 00:00:00 UTC
	 */

	public long getEndEpoch(){
		try {
			Calendar c = Calendar.getInstance(TimeZone.getDefault());
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(_journey.getOperatingDay()));
			c.set(Calendar.HOUR, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
			c.set(Calendar.MILLISECOND, 0);
			c.add(Calendar.SECOND, _journey.getDeparturetime());
			c.add(Calendar.SECOND, _journey.getTimedemandgroup().getPoints().get(_journey.getTimedemandgroup().getPoints().size()-1).getTotaldrivetime());
			if (posinfo != null && posinfo.getPunctuality() != null){
				c.add(Calendar.SECOND, Math.abs(posinfo.getPunctuality()));
			}
			return c.getTimeInMillis()/1000;
		} catch (ParseException e) {
			return -1;
		}
	}


	private StopTimeEvent.Builder stopTimeEventArrival(TimeDemandGroupPoint tpt,JourneyPatternPoint pt, int punctuality){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		stopTimeEvent.setDelay(punctuality);
		stopTimeEvent.setTime(_journey.getDepartureEpoch()+tpt.getTotaldrivetime()+punctuality);
		return stopTimeEvent;
	}


	private TripUpdate.Builder filter(TripUpdate.Builder tripUpdate){
		if (tripUpdate.getStopTimeUpdateCount() == 0)
			return tripUpdate;
		tripUpdate.getStopTimeUpdateOrBuilderList();
		long lastTime = Long.MAX_VALUE;
		for (int i = tripUpdate.getStopTimeUpdateCount()-1; i >= 0; i--){ //Filter negative dwells and stoptimes
			StopTimeUpdate.Builder update = tripUpdate.getStopTimeUpdateBuilder(i);
			if (update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.NO_DATA || 
					update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED ||
					update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate)){
				continue;
			}
			if (!update.hasDeparture() || !update.hasArrival()){
				//System.out.println(tripUpdate.build());
				//System.out.println(update.build());
				_log.error("Departure or arrival is missing");
			}
			if (update.getDeparture().getTime() > lastTime){
				int offset = (int) (lastTime - update.getDeparture().getTime());
				update.getDepartureBuilder().setTime(update.getDeparture().getTime()+offset);
				update.getDepartureBuilder().setDelay((int)(update.getDepartureBuilder().getTime()-_journey.getDepartureTime(update.getStopSequence())));
			}
			lastTime = update.getDeparture().getTime();
			if (update.getArrival().getTime() > lastTime){
				int offset = (int) (lastTime - update.getArrival().getTime());
				update.getArrivalBuilder().setTime(update.getArrival().getTime()+offset);
				update.getArrivalBuilder().setDelay((int)(update.getArrivalBuilder().getTime()-_journey.getArrivalTime(update.getStopSequence())));
			}
			lastTime = update.getArrival().getTime();
		}
		ArrayList<StopTimeUpdate.Builder> updates = new ArrayList<StopTimeUpdate.Builder>();
		int lastDelay = Integer.MIN_VALUE;
		StopTimeUpdate.ScheduleRelationship lastSchedule = StopTimeUpdate.ScheduleRelationship.SCHEDULED;
		boolean hadStopTimeExtension = false;
		List<StopTimeUpdate.Builder> unfilteredUpdates = tripUpdate.getStopTimeUpdateBuilderList();
		for (int i = 0; i < unfilteredUpdates.size(); i++){
			StopTimeUpdate.Builder update = unfilteredUpdates.get(i);
			if (update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.NO_DATA || 
					update.getScheduleRelationship() == StopTimeUpdate.ScheduleRelationship.SKIPPED ||
					update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate)){
				if (update.hasArrival()){
					update.clearArrival();
				}
				if (update.hasDeparture()){
					update.clearDeparture();
				}
				updates.add(update); //No data
				lastDelay = Integer.MIN_VALUE;
				lastSchedule = update.hasScheduleRelationship() ? StopTimeUpdate.ScheduleRelationship.SCHEDULED :
					update.getScheduleRelationship();
				hadStopTimeExtension = update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
				continue;
			}
			boolean override = lastSchedule != update.getScheduleRelationship() ||
					hadStopTimeExtension != update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
			if (update.hasArrival()){
				if ((update.getArrival().getDelay() == lastDelay && !override ) || 
						(i == 0 && update.getDeparture().getDelay() == update.getArrival().getDelay())){
					update.clearArrival();
				}else{
					lastDelay = update.getArrival().getDelay();
				}
			}
			if (update.hasDeparture()){
				if (update.getDeparture().getDelay() == lastDelay && (i != 0) && !override){
					update.clearDeparture();
				}else{
					lastDelay = update.getDeparture().getDelay();
				}
			}
			if (update.hasArrival() || update.hasDeparture()){
				updates.add(update);
			}
			lastSchedule = update.hasScheduleRelationship() ? StopTimeUpdate.ScheduleRelationship.SCHEDULED :
				update.getScheduleRelationship();
			hadStopTimeExtension = update.hasExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate);
		}
		tripUpdate.clearStopTimeUpdate();
		for (StopTimeUpdate.Builder update: updates){
			tripUpdate.addStopTimeUpdate(update);
		}
		return tripUpdate;
	}

	private StopTimeUpdate.Builder recordedTimes(TimeDemandGroupPoint tpt, JourneyPatternPoint pt){
		if (!RECORD_TIMES)
			return null;
		StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
		boolean stopcanceled = _journey.isCanceled();;
		boolean destChanged = false;
		if (mutations.containsKey(pt.getPointorder())){ // Check if mutation exists with cancel
			for (Mutation m : mutations.get(pt.getPointorder())){
				if (m.getMutationtype() == MutationType.SHORTEN){
					stopcanceled = true;
				}
				if (m.getMutationtype() == MutationType.CHANGEDESTINATION && !destChanged){
					destChanged = true;
					String destination = m.getDestinationname50();
					if (destination == null){
						destination = m.getDestinationname16();
					}
					if (destination == null){
						destination = m.getDestinationdisplay16();
					}
					if (destination != null){
						OVapiStopTimeUpdate.Builder ovapiStopTimeUpdate = OVapiStopTimeUpdate.newBuilder();
						ovapiStopTimeUpdate.setStopHeadsign(destination);
						stopTimeUpdate.setExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate, ovapiStopTimeUpdate.build());
					}
				}
			}
		}
		stopTimeUpdate.setStopSequence(pt.getPointorder());
		stopTimeUpdate.setStopId(pt.getPointref().toString());
		if (!pt.isScheduled())
			return null; // Dummy point
		if (stopcanceled){
			stopTimeUpdate.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
			return stopTimeUpdate;
		}
		if (realizedArrivals.containsKey(pt.getPointorder()))
			stopTimeUpdate.setArrival(stopTimeEventArrivalRecorded(tpt,realizedArrivals.get(pt.getPointorder())));
		if (realizedDepartures.containsKey(pt.getPointorder()))
			stopTimeUpdate.setDeparture(stopTimeEventDepartureRecorded(tpt,realizedDepartures.get(pt.getPointorder())));
		if ((pt.isWaitpoint() ||pt.getPointorder() <= 1) && stopTimeUpdate.hasArrival() && stopTimeUpdate.getArrival().getDelay() < 0 && !stopTimeUpdate.hasDeparture()){
			StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
			stopTimeEvent.setTime(_journey.getDepartureTime(tpt.getPointorder()));
			stopTimeEvent.setDelay(0);
			stopTimeUpdate.setDeparture(stopTimeEvent);
		}
		if (stopTimeUpdate.hasDeparture() && !stopTimeUpdate.hasArrival()){
			StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
			long time = stopTimeUpdate.getDeparture().getTime();
			int delay = stopTimeUpdate.getDeparture().getDelay();
			long targettime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime();
			if (delay > MIN_PUNCTUALITY*2){
				if (tpt.getStopwaittime() == 0){
					stopTimeEvent.setTime(time);
					stopTimeEvent.setDelay(delay);
					
				}else if (delay <= 0){
					stopTimeEvent.setTime(targettime);
					stopTimeEvent.setDelay(0);
				}else{
					int waittime = 	Math.min(tpt.getStopwaittime(),MIN_STOPWAITTIME);
					stopTimeEvent.setTime(time-waittime);
					stopTimeEvent.setDelay((int)(targettime-(time-waittime)));
				}
			}else{
				stopTimeEvent.setTime(targettime);
				stopTimeEvent.setDelay(0);
			}
			stopTimeUpdate.setArrival(stopTimeEvent);
		}
		if (!stopTimeUpdate.hasDeparture() && stopTimeUpdate.hasArrival()){
			StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
			long time = stopTimeUpdate.getArrival().getTime();
			int delay = stopTimeUpdate.getArrival().getDelay();
			time += tpt.getStopwaittime();
			if (delay < 0 && pt.isWaitpoint()){
				delay = 0;
				time = _journey.getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
			}
			if (delay > MIN_PUNCTUALITY*2){
				stopTimeEvent.setTime(time+tpt.getStopwaittime());
				stopTimeEvent.setDelay(delay);
				stopTimeUpdate.setDeparture(stopTimeEvent);
			}else{
				long targettime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime();
				stopTimeEvent.setTime(targettime);
				stopTimeEvent.setDelay(0);
				stopTimeUpdate.setDeparture(stopTimeEvent);
			}
		}
		if (!stopTimeUpdate.hasArrival() && !stopTimeUpdate.hasDeparture()){
			StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
			long targettime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime();
			stopTimeEvent.setTime(targettime);
			stopTimeEvent.setDelay(0);
			stopTimeUpdate.setArrival(stopTimeEvent);
			targettime += tpt.getStopwaittime();
			stopTimeEvent.setTime(targettime);
			stopTimeEvent.setDelay(0);
			stopTimeUpdate.setDeparture(stopTimeEvent);
		}
		if (!stopTimeUpdate.hasArrival() || !stopTimeUpdate.hasDeparture()){
			_log.error("Missing \n{}",stopTimeUpdate.build());
		}
		return stopTimeUpdate;
	}


	public TripUpdate.Builder updateTimes(KV6posinfo posinfo) {
		boolean passed = posinfo.getMessagetype() != KV6posinfo.Type.DELAY;
		int punctuality = Math
				.max(MIN_PUNCTUALITY, posinfo.getPunctuality() == null ? 0
						: posinfo.getPunctuality());
		TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();
		tripUpdate.setTrip(_journey.tripDescriptor());
		tripUpdate.setTimestamp(posinfo.getTimestamp());
		if (_journey.isCanceled()){
			return tripUpdate;
		}
		VehicleDescriptor.Builder vehicleDesc = posinfo.getVehicleDescription();
		if (vehicleDesc != null)
			tripUpdate.setVehicle(vehicleDesc);
		switch (posinfo.getMessagetype()) {// These types do not contain // information regarding punctuality
		case INIT:
			realizedArrivals = Maps.newHashMap();
			realizedDepartures = Maps.newHashMap();
		case END:
		case OFFROUTE:
			if (getPosinfo() != null && !hasMutations() && getPosinfo().getMessagetype() != Type.OFFROUTE)
				return null; //We've already sent out NO_DATE
			for (int i = 0; i < _journey.getTimedemandgroup().getPoints().size(); i++) {
				TimeDemandGroupPoint tpt = _journey.getTimedemandgroup().getPoints().get(i);
				JourneyPatternPoint pt = _journey.getJourneypattern().getPoint(tpt.pointorder);
				if (!pt.isScheduled())
					continue;
				StopTimeUpdate.Builder recordedTimes = recordedTimes(tpt,pt);
				if (recordedTimes != null){
					tripUpdate.addStopTimeUpdate(recordedTimes);
				}else if (posinfo.getMessagetype() == Type.OFFROUTE){
					StopTimeUpdate.Builder noData = StopTimeUpdate.newBuilder();
					noData.setStopSequence(pt.getPointorder());
					noData.setStopId(pt.getPointref().toString());
					noData.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
					tripUpdate.addStopTimeUpdate(noData);
					if (!hasMutations())
						break;
				}
			}
			this.posinfo = posinfo;
			tripUpdate = filter(tripUpdate);
			if (tripUpdate.getStopTimeUpdateCount() > 0)
				return tripUpdate;
		default:
			break;
		}
		int passageseq = 0;
		int elapsedtime = 0;
		for (int i = 0; i < _journey.getTimedemandgroup().getPoints().size(); i++) {
			TimeDemandGroupPoint tpt = _journey.getTimedemandgroup().getPoints().get(i);
			JourneyPatternPoint pt = _journey.getJourneypattern().getPoint(tpt.pointorder);
			if (pt.getOperatorpointref().equals(posinfo.getUserstopcode())) {
				if (posinfo.getPassagesequencenumber() - passageseq > 0) {
					passageseq++; // Userstop equal but posinfo relates to n-th
					// passing
				} else {
					elapsedtime = tpt.getTotaldrivetime()+tpt.getStopwaittime();
					passed = false;
					switch (posinfo.getMessagetype()) {
					case ARRIVAL:
						if (RECORD_TIMES)
							realizedArrivals.put(pt.getPointorder(), posinfo.getTimestamp());
						long targetArrivalTime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime();
						int newPunctuality = (int)(posinfo.getTimestamp()-targetArrivalTime);
						if (newPunctuality > -60 && newPunctuality - punctuality < 600){
							punctuality = newPunctuality;
						}
						if ((pt.isWaitpoint() || i == 0)	&& punctuality < 0)
							punctuality = 0;
						break;
					case DEPARTURE:
						long targetDepartureTime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
						newPunctuality = (int)(posinfo.getTimestamp()-targetDepartureTime);
						if (RECORD_TIMES && (!pt.isWaitpoint() || newPunctuality > 0))
							realizedDepartures.put(pt.getPointorder(), posinfo.getTimestamp());
						if ((pt.isWaitpoint() || i == 0)	&& punctuality < 0)
							punctuality = 0;
						break;
					case ONSTOP:
						if ((pt.isWaitpoint() || i == 0)	&& punctuality < 0)
							punctuality = 0;
						targetDepartureTime = _journey.getDepartureEpoch()+tpt.getTotaldrivetime()+tpt.getStopwaittime();
						newPunctuality = (int)(posinfo.getTimestamp()-targetDepartureTime);
						if (newPunctuality > -30 && newPunctuality - punctuality < 600){
							punctuality = newPunctuality;
						}
						break;
					default:
						break;
					}
				}
				StopTimeUpdate.Builder recorded = recordedTimes(tpt,pt);
				if (recorded != null){
					tripUpdate.addStopTimeUpdate(recorded);
				}
			} else if (!passed) { //Stops not visted by the vehicle
				StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
				stopTimeUpdate.setStopSequence(tpt.getPointorder());
				stopTimeUpdate.setStopId(pt.getPointref().toString());
				stopTimeUpdate.setArrival(stopTimeEventArrival(tpt,pt,punctuality));
				boolean stopcanceled = _journey.isCanceled();
				boolean destChanged = false;
				if (mutations.containsKey(tpt.getPointorder())){ // Check if mutation exists with cancel
					for (Mutation m : mutations.get(tpt.getPointorder())){
						if (m.getMutationtype() == MutationType.SHORTEN){
							stopcanceled = true;
						}
						if (m.getMutationtype() == MutationType.CHANGEDESTINATION && !destChanged){
							destChanged = true;
							String destination = m.getDestinationname50();
							if (destination == null){
								destination = m.getDestinationname16();
							}
							if (destination == null){
								destination = m.getDestinationdisplay16();
							}
							if (destination != null){
								OVapiStopTimeUpdate.Builder ovapiStopTimeUpdate = OVapiStopTimeUpdate.newBuilder();
								ovapiStopTimeUpdate.setStopHeadsign(destination);
								stopTimeUpdate.setExtension(GtfsRealtimeOVapi.ovapiStopTimeUpdate, ovapiStopTimeUpdate.build());
							}
						}
					}
				}
				stopTimeUpdate.setScheduleRelationship(
						stopcanceled ? StopTimeUpdate.ScheduleRelationship.SKIPPED
								: StopTimeUpdate.ScheduleRelationship.SCHEDULED);
				if (pt.isWaitpoint() && punctuality < 0)
					punctuality = 0;
				if (tpt.getStopwaittime() != 0 && punctuality > 0) { //Minimize delay by cutting into dwells
					int stopwaittime = tpt.getStopwaittime();
					if (stopwaittime > 100 ) {
						punctuality -= stopwaittime-MIN_STOPWAITTIME;
						punctuality = Math.max(0, punctuality);
					}
				}
				stopTimeUpdate.setDeparture(stopTimeEventDeparture(tpt,pt,punctuality));
				if (pt.isScheduled()){
					tripUpdate.addStopTimeUpdate(stopTimeUpdate);
				}

				punctuality = stopTimeUpdate.getDeparture().getDelay();
				if (i+1 < _journey.getTimedemandgroup().getPoints().size()){
					TimeDemandGroupPoint ntpt = _journey.getTimedemandgroup().getPoints().get(i+1);
					JourneyPatternPoint npt = _journey.getJourneypattern().getPoint(ntpt.pointorder);
					int distanceToNext = npt.getDistancefromstartroute() - pt.getDistancefromstartroute();
					int secondsToNext = ntpt.getTotaldrivetime() - (tpt.getTotaldrivetime()+tpt.getStopwaittime());
					int speed = DEFAULT_SPEED;
					if (distanceToNext > 10000) {
						speed = LONGHAUL_SPEED;
					} else if (distanceToNext < 1000) {
						speed = SHORTHAUL_SPEED;
					}
					int fastest = distanceToNext / speed;
					if ((punctuality > 0 || secondsToNext < fastest) && i != _journey.getTimedemandgroup().getPoints().size() - 1) {
						punctuality -= (secondsToNext - fastest);
						if (punctuality < 0) {
							punctuality = 0;
						}
					} else if (punctuality < 0 && i != _journey.getTimedemandgroup().getPoints().size() - 1) {
						punctuality = decayeddelay(punctuality,
								tpt.getTotaldrivetime() - elapsedtime);
					}
					if (Math.abs(punctuality) < PUNCTUALITY_FLOOR) {
						punctuality = 0;
					}
				}
			}else{ //JourneyPatternPoint has been passed.
				StopTimeUpdate.Builder recorded = recordedTimes(tpt,pt);
				if (recorded != null){
					tripUpdate.addStopTimeUpdate(recorded);
				}
			}
		}
		this.posinfo = posinfo;
		tripUpdate = filter(tripUpdate);
		if (tripUpdate.getStopTimeUpdateCount() > 0)
			return tripUpdate;
		else
			return null;
	}

	private int decayeddelay(int delay, int elapsedtime) {
		if (delay == 0)
			return 0;
		double vlamba = 1.0 / 500.0;
		double decay = Math.exp(-vlamba * elapsedtime);
		int decayeddelay = (int) (decay * delay);
		return decayeddelay;

	}

	private void parseMutateJourney(Long timestamp, Mutation m) {
		switch (m.getMutationtype()) {
		case CANCEL:
			_journey.setCanceled(true);
			break;
		case RECOVER:
			_journey.setCanceled(false);
			break;
		default:
			break;

		}
	}

	
	private void parseMutateJourneyStop(Long timestamp, Mutation m)
			throws StopNotFoundException {
		JourneyPatternPoint pst = _journey.getJourneyStop(m.getUserstopcode(),m.getPassagesequencenumber());
		if (pst == null) {
			throw new StopNotFoundException(m.toString());
		}
		if (!mutations.containsKey(pst.getPointorder()))
			mutations.put(pst.getPointorder(), new ArrayList<Mutation>());
		switch (m.getMutationtype()) {
		case CHANGEDESTINATION:
		case CHANGEPASSTIMES:
		case LAG:
		case RECOVER:
		case CANCEL:
		case SHORTEN:
			mutations.get(pst.getPointorder()).add(m);
			break;
		default:
			_log.info("Unknown mutationtype {}",m);
			break;
		}
	}
	
	public FeedEntity vehiclePosition(String id,JourneyProcessor journey,KV6posinfo posinfo,RIDservice ridService,GeometryService geomService){
		FeedEntity.Builder feedEntity = FeedEntity.newBuilder();
		feedEntity.setId(id);
		VehiclePosition.Builder vehiclePosition = VehiclePosition.newBuilder();
		int delay = posinfo.getPunctuality() == null ? 0 : posinfo.getPunctuality();
		switch (posinfo.getMessagetype()){
		case END:
			return null;
		case DELAY:
			TimeDemandGroupPoint firstTimePoint = _journey.getTimedemandgroup().getPoints().get(0);
			JourneyPatternPoint firstPatternPoint = _journey.getJourneypattern().getPoint(firstTimePoint.getPointorder());
			vehiclePosition.setStopId(firstPatternPoint.getPointref().toString());
			vehiclePosition.setCurrentStatus(VehicleStopStatus.IN_TRANSIT_TO);
			vehiclePosition.setCurrentStopSequence(firstTimePoint.getPointorder());
			delay = Math.max(0, delay);
			break;
		case INIT:
		case ARRIVAL:
		case ONSTOP:
			for (JourneyPatternPoint point : _journey.getJourneypattern().getPoints()){
				if (point.getOperatorpointref().equals(posinfo.getUserstopcode())){
					vehiclePosition.setStopId(point.getPointref().toString());
					vehiclePosition.setCurrentStopSequence(point.getPointorder());
					vehiclePosition.setCurrentStatus(VehicleStopStatus.STOPPED_AT);
					StopPoint sp = ridService.getStopPoint(point.getPointref());
					if ((posinfo.getRd_x() == null || posinfo.getRd_x() == -1) && sp != null){
						Builder position = Position.newBuilder();
						position.setLatitude(sp.getLatitude());
						position.setLongitude(sp.getLongitude());
						vehiclePosition.setPosition(position);
					}
					if (point.isWaitpoint() && delay < 0){
						delay = 0;
					}
				}
			}
			break;
		case DEPARTURE:
		case OFFROUTE:
		case ONROUTE:
			boolean passed = false;
			for (JourneyPatternPoint point : _journey.getJourneypattern().getPoints()){
				if (point.getOperatorpointref().equals(posinfo.getUserstopcode())){
					passed = true;
					StopPoint sp = ridService.getStopPoint(point.getPointref());
					vehiclePosition.setCurrentStopSequence(point.getPointorder());
					if (posinfo.getMessagetype() == Type.DEPARTURE && sp != null){
						Builder position = Position.newBuilder();
						position.setLatitude(sp.getLatitude());
						position.setLongitude(sp.getLongitude());
						vehiclePosition.setPosition(position);
						if (delay < 0 &&  point.isWaitpoint()){
							delay = 0;
						}
					}
				}else if (passed && point.isScheduled()){
					vehiclePosition.setStopId(point.getPointref().toString());
					vehiclePosition.setCurrentStopSequence(point.getPointorder());
					vehiclePosition.setCurrentStatus(VehicleStopStatus.IN_TRANSIT_TO);
				}
			}
			break;
		}
		if (posinfo.getRd_x() != null){
			Position position = geomService.toWGS84(posinfo.getRd_x(), posinfo.getRd_y());
			if (position != null)
				vehiclePosition.setPosition(position);
		}
		TripDescriptor.Builder tripDescription = _journey.tripDescriptor();
		if (posinfo.getReinforcementnumber() > 0){
			tripDescription.setScheduleRelationship(ScheduleRelationship.ADDED);
		}
		vehiclePosition.setTrip(tripDescription);
		if (posinfo.getVehicleDescription() != null)
			vehiclePosition.setVehicle(posinfo.getVehicleDescription());
		vehiclePosition.setTimestamp(posinfo.getTimestamp());
		if (posinfo.getPunctuality() != null){
			OVapiVehiclePosition.Builder ovapiVehiclePosition = OVapiVehiclePosition.newBuilder();
			if (vehiclePosition.hasCurrentStopSequence() && vehiclePosition.getCurrentStopSequence() <= 1 && delay < 0){
				delay = 0;
			}
			ovapiVehiclePosition.setDelay(delay);
			vehiclePosition.setExtension(GtfsRealtimeOVapi.ovapiVehiclePosition, ovapiVehiclePosition.build());
		}
		feedEntity.setVehicle(vehiclePosition);
		return feedEntity.build();
	}	

	public TripUpdate.Builder update(ArrayList<KV17cvlinfo> cvlinfos) {
		long timestamp = 0;
		if (cvlinfos.size() == 0){
			return null;
		}
		mutations.clear();
		_journey.setCanceled(false);
		for (KV17cvlinfo cvlinfo : cvlinfos) {
			for (Mutation mut : cvlinfo.getMutations()) {
				try {
					timestamp = Math.max(timestamp, cvlinfo.getTimestamp());
					switch (mut.getMessagetype()) {
					case KV17MUTATEJOURNEY:
						parseMutateJourney(cvlinfo.getTimestamp(), mut);
						continue;
					case KV17MUTATEJOURNEYSTOP:
						parseMutateJourneyStop(cvlinfo.getTimestamp(), mut);
						continue;
					}
				} catch (Exception e) {
					_log.error("Error applying KV17",e);
				}
			}
		}
		int posinfoAge = (posinfo == null) ? Integer.MAX_VALUE : 
			(int)(Utils.currentTimeSecs()-posinfo.getTimestamp());
		if (timestamp == 0)
			timestamp = Utils.currentTimeSecs();
		if (posinfo != null && posinfoAge < 120){
			TripUpdate.Builder timeUpdate = updateTimes(posinfo);
			timeUpdate.setTimestamp(timestamp);
			return timeUpdate;
		}else{
			KV6posinfo posinfo = new KV6posinfo();
			posinfo.setMessagetype(Type.DELAY); //Fake KV6posinfo to get things moving
			posinfo.setPunctuality(0);
			posinfo.setTimestamp(timestamp);
			return updateTimes(posinfo);
		}
	}

	public TripUpdate.Builder update(KV6posinfo posinfo) throws StopNotFoundException,UnknownKV6PosinfoType, TooEarlyException, TooOldException {
		long currentTime = Utils.currentTimeSecs();
		if (posinfo.getTimestamp()<currentTime-120){
			throw new TooOldException(posinfo.toString());
		}
		long departureTime = _journey.getDepartureEpoch();
		if (currentTime < departureTime){
			int timeDeltaSeconds = (int)(departureTime-Utils.currentTimeSecs());
			if (timeDeltaSeconds>=3600){
				switch(posinfo.getMessagetype()){
				case INIT:
					break;
				default:
					throw new TooEarlyException(posinfo.toString());
				}
			}
		}
		if (posinfo.getUserstopcode() != null
				&& !_journey.getJourneypattern().contains(posinfo.getUserstopcode())) {
			throw new StopNotFoundException(posinfo.toString());
		}
		return updateTimes(posinfo);
	}		
}
