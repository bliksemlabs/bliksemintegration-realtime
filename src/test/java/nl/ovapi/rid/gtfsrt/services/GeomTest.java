package nl.ovapi.rid.gtfsrt.services;
import static org.junit.Assert.*;

import nl.ovapi.rid.gtfsrt.services.GeometryService;

import org.junit.Test;

import com.google.transit.realtime.GtfsRealtime.Position;


public class GeomTest {

	@Test
	public void rijksdriehoek_convert(){
		GeometryService gs = new GeometryService();
		Position point = gs.toWGS84(0,0);
		assertNull(point);
		point = gs.toWGS84(79417, 458947);
		assertEquals(point.getLatitude(),52.113581,0.00001);
		assertEquals(point.getLongitude(),4.283739,0.00001);
	}

}
