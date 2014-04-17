package nl.ovapi.arnu;

import java.util.ArrayList;
import java.util.Map;

import nl.ovapi.rid.gtfsrt.services.RIDservice;

import com.google.common.collect.Maps;

public class FakeRidService extends RIDservice{
	
	private Map<String, ArrayList<Long>> userstops = Maps.newHashMapWithExpectedSize(50000);
	
	public FakeRidService(){
		addFakeStations();
	}
	
	/**
	 * 
	 * @param station NS stationcode 
	 * @return Long of identifier of stoppoint with undefined platform for that station
	 */
	@Override
	public Long getRailStation(String station, String platformCode){
		if (platformCode == null){
			platformCode = "0";
		}
		String id = String.format("IFF:%s:0", station);
		ArrayList<Long> results = userstops.get(id);
		if (results == null || results.size() == 0){
			return null;
		}
		return results.get(0);
	}
	
	private void addFakeStation(String stationCode, Long id){
		ArrayList<Long> station = new ArrayList<Long>();
		station.add(id);
		userstops.put(String.format("IFF:%s:0", stationCode), station);
	}
	
	public void addFakeStations(){
		addFakeStation("btl",1L);
		addFakeStation("vg",2L);
		addFakeStation("ht",3L);
		addFakeStation("ehb",4L);
		addFakeStation("bet",5L);
	}
}