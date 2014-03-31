package nl.ovapi.rid.gtfsrt;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import nl.ovapi.arnu.ARNUexporter;
import nl.ovapi.rid.gtfsrt.services.ARNUritInfoToGtfsRealTimeServices;
import nl.ovapi.rid.gtfsrt.services.BisonToGtfsRealtimeService;
import nl.ovapi.rid.gtfsrt.services.GeometryService;
import nl.ovapi.rid.gtfsrt.services.KV78TurboToPseudoKV6Service;

import org.onebusaway.gtfs_realtime.exporter.GtfsRealtimeExporterModule;
import org.onebusaway.guice.jsr250.JSR250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class BisonToGtfsRealtimeModule extends AbstractModule {

	public static void addModuleAndDependencies(Set<Module> modules) {
		modules.add(new BisonToGtfsRealtimeModule());
		GtfsRealtimeExporterModule.addModuleAndDependencies(modules);
		JSR250Module.addModuleAndDependencies(modules);
	}

	@Override
	protected void configure() {
		bind(ARNUexporter.class);
		bind(KV78TurboToPseudoKV6Service.class);
		bind(BisonToGtfsRealtimeService.class);
		bind(ARNUritInfoToGtfsRealTimeServices.class);
		bind(GeometryService.class);
		bind(ScheduledExecutorService.class).toInstance(
				Executors.newSingleThreadScheduledExecutor());
	}

	/**
	 * Implement hashCode() and equals() such that two instances of the module
	 * will be equal.
	 */
	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		return this.getClass().equals(o.getClass());
	}
}