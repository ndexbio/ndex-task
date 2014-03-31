package org.ndexbio.task.aspect;



import java.util.concurrent.TimeUnit;

import javax.ws.rs.PathParam;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.task.event.NdexEventBus;
import org.ndexbio.task.event.NdexExportTaskEvent;

import com.google.common.base.Stopwatch;

/*
 * aspect to monitor methods in the NetworkService class
 */
public aspect NetworkServiceAspect {
	/*
	 * public Network getEdgesByCitations(@PathParam("networkId") final String networkId,
			@PathParam("skip") final int skip, @PathParam("top") final int top, 
			final String[] citations)
	 */
	
	//++++++++++++getEdgesByCitation
	//pointcut
	pointcut getEdgesByCitations( String networkId, int skip,  int top, 
			 String[] citations) :
		execution (
		public Network org.ndexbio.rest.services.NetworkService.getEdgesByCitations
			( String,  int,  int,  String[]) 
					throws IllegalArgumentException, NdexException)
		&& args( networkId, skip, top,citations);
	// advice
	Network around (String networkId, int skip, int top, String[] citations) :
		getEdgesByCitations(networkId, skip ,top, citations) {
		Stopwatch sw = Stopwatch.createStarted();
		Network network = proceed(networkId,skip,top,citations);
		long time = sw.stop().elapsed(TimeUnit.MILLISECONDS);
		NdexExportTaskEvent event = new NdexExportTaskEvent();
		event.setEntityCount(network.getEdges().size());
		event.setEntityValue("edges");
		event.setMetric("edges");
		event.setValue(time);
		event.setUnits("milliseconds");
		event.setValue(time);
		NdexEventBus.INSTANCE.getEventBus().post(event);
		return network;	
		
		
	}
	
}
