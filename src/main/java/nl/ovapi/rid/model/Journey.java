package nl.ovapi.rid.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import nl.ovapi.exceptions.UnknownKV6PosinfoType;
import nl.ovapi.rid.model.JourneyPattern.JourneyPatternPoint;
import nl.ovapi.rid.model.TimeDemandGroup.TimeDemandGroupPoint;

import com.google.common.collect.Maps;
import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent.Builder;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
@ToString()
public class Journey {
	@Getter
	@Setter
	private Long id;
	@Getter
	@Setter
	private JourneyPattern journeypattern;
	@Getter
	@Setter
	private TimeDemandGroup timedemandgroup;
	@Getter
	@Setter
	private Integer departuretime;
	@Getter
	@Setter
	private Boolean wheelchairaccessible;
	@Getter
	@Setter
	private String agencyId;
	@Getter
	@Setter
	private String operatingDay;

	@Getter
	@Setter
	private KV6posinfo posinfo;

	@Getter
	@Setter
	private boolean isCanceled;

	private Map<Integer, ArrayList<Mutation>> mutations;

	@Getter
	@Setter
	private Map<Integer, KV6posinfo> reinforcements;

	public Journey() {
		mutations = Maps.newHashMap();
		reinforcements = Maps.newHashMap();
	}

	private static final int PUNCTUALITY_FLOOR = 15; // seconds
	private static final int DEFAULT_SPEED = (int) (80 / 3.6); // meters per
	// seconds
	private static final int LONGHAUL_SPEED = (int) (100 / 3.6); // meters per
	// seconds
	private static final int SHORTHAUL_SPEED = (int) (60 / 3.6); // meters per
	// seconds

	private static final int MIN_PUNCTUALITY = -300; // Minimum allowed
	// punctuality.


	public TripDescriptor.Builder tripDescriptor(){
		TripDescriptor.Builder tripDescriptor = TripDescriptor.newBuilder();
		tripDescriptor.setStartDate(operatingDay);
		tripDescriptor.setTripId(id.toString());
		tripDescriptor.setScheduleRelationship(isCanceled ? ScheduleRelationship.CANCELED : ScheduleRelationship.SCHEDULED);
		return tripDescriptor;
	}

	public StopTimeEvent.Builder stopTimeEventArrival(JourneyPatternPoint pt, int punctuality){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		stopTimeEvent.setDelay(punctuality);
		return stopTimeEvent;
	}

	public boolean hasMutations(){
		return mutations.size() > 0 || isCanceled;
	}

	/**
	 * @return Whether journey is currently planned to ride or is still riding. 
	 */

	public boolean isCurrent(){
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		long endTime = getDepartureEpoch() + (timedemandgroup.points.get(timedemandgroup.points.size()-1).getTotaldrivetime()) * 1000;
		if (posinfo != null){
			endTime += Math.abs(posinfo.getPunctuality()*1000);
		}
		return (System.currentTimeMillis()+5*60*1000) > endTime; //Whether end-of-trip is 5 minutes ago 
	}

	public StopTimeEvent.Builder stopTimeEventDeparture(JourneyPatternPoint pt, int punctuality){
		StopTimeEvent.Builder stopTimeEvent = StopTimeEvent.newBuilder();
		if (mutations.containsKey(pt.getPointorder())){
			for (Mutation m : mutations.get(pt.getPointorder())){
				if (m.getMutationtype() == MutationType.LAG){
					punctuality = Math.max(punctuality, m.getLagtime());
				}
			}
		}
		stopTimeEvent.setDelay(punctuality);
		return stopTimeEvent;
	}

	public long getDepartureEpoch(){
		try{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			return df.parse(operatingDay).getTime() + (departuretime*1000);
		}catch (Exception e){}
		return -1;
	}

	public TripUpdate.Builder updateTimes(KV6posinfo posinfo) {
		boolean passed = posinfo.getMessagetype() != KV6posinfo.Type.DELAY;
		int punctuality = Math
				.max(MIN_PUNCTUALITY, posinfo.getPunctuality() == null ? 0
						: posinfo.getPunctuality());
		TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();
		tripUpdate.setTrip(tripDescriptor());
		tripUpdate.setTimestamp(posinfo.getTimestamp());
		if (isCanceled){
			return tripUpdate;
		}
		VehicleDescriptor.Builder vehicleDesc = posinfo.getVehicleDescription();
		if (vehicleDesc != null)
			tripUpdate.setVehicle(vehicleDesc);
		switch (posinfo.getMessagetype()) {// These types do not contain // information regarding punctuality
		case INIT:
		case END:
		case OFFROUTE:
			return tripUpdate;
		default:
			break;
		}
		int passageseq = 0;
		int elapsedtime = 0;
		boolean nullterminated = false;
		for (int i = 0; i < timedemandgroup.points.size(); i++) {
			TimeDemandGroupPoint tpt = timedemandgroup.points.get(i);
			JourneyPatternPoint pt = journeypattern.getPoint(tpt.pointorder);
			if (pt.getOperatorpointref().equals(posinfo.getUserstopcode())) {
				if (posinfo.getPassagesequencenumber() - passageseq > 0) {
					passageseq++; // Userstop equal but posinfo relates to n-th
					// passing
				} else {
					elapsedtime = tpt.getTotaldrivetime()+tpt.getStopwaittime();
					passed = false;
					switch (posinfo.getMessagetype()) {
					case ARRIVAL:
					case ONSTOP:
						if ((pt.iswaitpoint || i == 0)	&& punctuality < 0)
							punctuality = 0;
						break;
					case DEPARTURE:
						if ((pt.iswaitpoint || i == 0)	&& punctuality < 0)
							punctuality = 0;
						break;
					default:
						break;
					}
				}
			} else if (!passed) {
				StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
				stopTimeUpdate.setStopSequence(tpt.getPointorder());
				stopTimeUpdate.setStopId(pt.getPointref().toString());
				stopTimeUpdate.setArrival(stopTimeEventArrival(pt,punctuality));
				boolean stopcanceled = isCanceled;
				if (mutations.containsKey(tpt.getPointorder())){ // Check if mutation exists with cancel
					for (Mutation m : mutations.get(tpt.getPointorder())){
						if (m.getMutationtype() == MutationType.SHORTEN){
							stopcanceled = true;
						}
					}
				}
				stopTimeUpdate.setScheduleRelationship(
						stopcanceled ? StopTimeUpdate.ScheduleRelationship.SKIPPED
								: StopTimeUpdate.ScheduleRelationship.SCHEDULED);
				if (pt.getIswaitpoint() && punctuality < 0)
					punctuality = 0;
				if (tpt.getStopwaittime() != 0) {
					int stopwaittime = tpt.getStopwaittime();
					if (stopwaittime > 20 && punctuality > 0) {
						punctuality -= Math.max(0, stopwaittime - 20);
						punctuality = Math.max(0, punctuality);
					}
				}
				stopTimeUpdate.setDeparture(stopTimeEventDeparture(pt,punctuality));
				if (!nullterminated || Math.abs(punctuality) > 0 || stopcanceled){
					if (punctuality == 0){
						nullterminated = true;
					}
					tripUpdate.addStopTimeUpdate(stopTimeUpdate);
				}
				punctuality = stopTimeUpdate.getDeparture().getDelay();
				if (punctuality > 0 && i != timedemandgroup.points.size() - 1) {
					TimeDemandGroupPoint ntpt = timedemandgroup.points.get(i+1);
					JourneyPatternPoint npt = journeypattern.getPoint(ntpt.pointorder);
					int distanceToNext = npt.getDistancefromstartroute() - pt.getDistancefromstartroute();
					int secondsToNext = ntpt.getTotaldrivetime() - (tpt.getTotaldrivetime()+tpt.getStopwaittime());
					int speed = DEFAULT_SPEED;
					if (distanceToNext > 10000) {
						speed = LONGHAUL_SPEED;
					} else if (distanceToNext < 1000) {
						speed = SHORTHAUL_SPEED;
					}
					int fastest = distanceToNext / speed;
					punctuality -= (secondsToNext - fastest);
					if (punctuality < 0) {
						punctuality = 0;
					}
				} else if (punctuality < 0 && i != timedemandgroup.points.size() - 1) {
					punctuality = decayeddelay(punctuality,
							tpt.getTotaldrivetime() - elapsedtime);
				}
				if (Math.abs(punctuality) < PUNCTUALITY_FLOOR) {
					punctuality = 0;
				}
			}
		}
		return tripUpdate;
	}

	public int decayeddelay(int delay, int elapsedtime) {
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
			isCanceled = true;
			break;
		case RECOVER:
			isCanceled = false;
			break;
		default:
			break;

		}
	}

	private JourneyPatternPoint getJourneyStop (String userstopcode,int passageSequencenumber){
		for (int i = 0; i < timedemandgroup.points.size(); i++) {
			TimeDemandGroupPoint tpt = timedemandgroup.points.get(i);
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

	private void parseMutateJourneyStop(Long timestamp, Mutation m)
			throws StopNotFoundException {
		JourneyPatternPoint pst = getJourneyStop(m.getUserstopcode(),m.getPassagesequencenumber());
		if (pst == null) {
			throw new StopNotFoundException(m.toString());
		}
		mutations.put(pst.getPointorder(), new ArrayList<Mutation>());
		switch (m.getMutationtype()) {
		case CHANGEDESTINATION:
			break;
		case CHANGEPASSTIMES:
			mutations.get(pst.getPointorder()).add(m);
			break;
		case LAG:
			mutations.get(pst.getPointorder()).add(m);
			break;
		case MUTATIONMESSAGE:
			mutations.get(pst.getPointorder()).add(m);
			break;
		case SHORTEN:
			mutations.get(pst.getPointorder()).add(m);
			break;
		case RECOVER:
		case CANCEL:
		default:
			break;
		}
	}

	public TripUpdate.Builder update(ArrayList<KV17cvlinfo> cvlinfos) {
		long timestamp = 0;
		if (cvlinfos.size() == 0){
			return null;
		}
		mutations.clear();
		for (KV17cvlinfo cvlinfo : cvlinfos) {
			for (Mutation mut : cvlinfo.getMutations()) {
				try {
					timestamp = Math.max(timestamp, cvlinfo.getTimestamp());
					switch (mut.getMessagetype()) {
					case KV17MUTATEJOURNEY:
						parseMutateJourney(cvlinfo.getTimestamp(), mut);
						break;
					case KV17MUTATEJOURNEYSTOP:
						parseMutateJourneyStop(cvlinfo.getTimestamp(), mut);
						break;
					default:
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		int posinfoAge = (int)((System.currentTimeMillis()-posinfo.getTimestamp()) / 1000);
		if (posinfo != null && posinfoAge < 120){
			TripUpdate.Builder timeUpdate = updateTimes(posinfo);
			timeUpdate.setTimestamp(cvlinfos.get(0).getTimestamp());
			return timeUpdate;
		}else{
			KV6posinfo posinfo = new KV6posinfo();
			posinfo.setMessagetype(Type.DELAY);
			posinfo.setPunctuality(0);
			posinfo.setTimestamp(cvlinfos.get(0).getTimestamp());
			return updateTimes(posinfo);
		}
	}

	public TripUpdate.Builder update(KV6posinfo posinfo) throws StopNotFoundException,UnknownKV6PosinfoType, TooEarlyException {
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(posinfo.getOperatingday()));
			c.add(Calendar.SECOND, getDeparturetime());
			if (System.currentTimeMillis() < c.getTimeInMillis()){
				int timedifference = (int)((c.getTimeInMillis()- System.currentTimeMillis())/1000);
				if (timedifference>=3600){
					switch(posinfo.getMessagetype()){
					case INIT:
					case ARRIVAL:
					case ONSTOP:
					case DELAY:
						break;
					default:
						throw new TooEarlyException(posinfo.toString());
					}
				}
			}
			if (posinfo.getUserstopcode() != null
					&& !journeypattern.contains(posinfo.getUserstopcode())) {
				throw new StopNotFoundException(posinfo.toString());
			}
			return updateTimes(posinfo);
		} catch (ParseException e) {}
		return null;
	}
}

