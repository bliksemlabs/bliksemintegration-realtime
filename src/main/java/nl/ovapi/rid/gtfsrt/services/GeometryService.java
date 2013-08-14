package nl.ovapi.rid.gtfsrt.services;

import javax.inject.Singleton;

import org.osgeo.proj4j.CRSFactory;
import org.osgeo.proj4j.CoordinateReferenceSystem;
import org.osgeo.proj4j.CoordinateTransform;
import org.osgeo.proj4j.CoordinateTransformFactory;
import org.osgeo.proj4j.ProjCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.Position;

@Singleton
public class GeometryService {
	private CoordinateReferenceSystem wgs84;
	private CoordinateReferenceSystem rijksdriehoek;
	private CoordinateTransform factory;
	private static final Logger _log = LoggerFactory.getLogger(GeometryService.class);


	public GeometryService(){
		wgs84 = new CRSFactory().createFromName("EPSG:4326");
		rijksdriehoek = new CRSFactory().createFromParameters("EPSG:28992", "+proj=sterea +lat_0=52.15616055555555 +lon_0=5.38763888888889 +k=0.9999079 +x_0=155000 +y_0=463000 +ellps=bessel +units=m +towgs84=565.2369,50.0087,465.658,-0.406857330322398,0.350732676542563,-1.8703473836068,4.0812 +no_defs no_defs");
		factory = new CoordinateTransformFactory().createTransform(rijksdriehoek,wgs84);
	}

	/**
	 * @param rd_x x-coordinate in range of -7000  and 300 000
	 * @param rd_y y-coordinate in range of 289000 and 629 000
	 * @return LatLng class with doubles for latitude and longitude in WGS84 projection\nNull if RD-coordinates out of rangeb
	 */

	public Position toWGS84(int rd_x,int rd_y){
		if (rd_x < -7000 || rd_x > 300000){
			return null;
		}
		if (rd_y < 289000 || rd_y > 629000){
			return null;
		}
		ProjCoordinate src = new ProjCoordinate(rd_x,rd_y);
		ProjCoordinate tgt = new ProjCoordinate();
		try{
			factory.transform(src, tgt);
			Position.Builder builder = Position.newBuilder();
			builder.setLatitude((float)tgt.y);
			builder.setLongitude((float)tgt.x);
			return builder.build();
		}catch(Exception e){
			_log.error("RD->WGS84 x={},y={}",rd_x,rd_y,e);
			return null;
		}
	}
}
