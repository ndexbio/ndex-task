package org.ndexbio.task.aspect;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.ndexbio.common.models.object.BaseTerm;
import org.ndexbio.common.models.object.Citation;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.task.event.NdexEventBus;
import org.ndexbio.task.event.NdexExportTaskEvent;

import com.google.common.base.Stopwatch;

/*
 * Aspect to support AspectJ pointcuts and advice for the NdexJVMDataModelService class
 */
public aspect DataModelServiceAspect {

	
	// pointcut for all methods in NdexJVMDataModelService class
	pointcut modelMethods() : execution (* org.ndexbio.task.service.NdexJVMDataModelService.*(..));
	
	//+++++++++++++++++ getNetworkById method monitoring
	// pointcut 
	pointcut getNetworkById(String id) : execution (
			public Network org.ndexbio.task.service.NdexJVMDataModelService.getNetworkById(String))
			&& args(id);
	
	
	// advice
	Network around (String id) : getNetworkById(id){
		Stopwatch sw = Stopwatch.createStarted();
		Network network = proceed(id);
		long time = sw.stop().elapsed(TimeUnit.MILLISECONDS);
		NdexExportTaskEvent event = new NdexExportTaskEvent();
		event.setEntityCount(network.getEdges().size());
		event.setEntityValue("edges");
		event.setOperation(thisJoinPointStaticPart.getSignature()
				.toShortString());
		event.setTaskType("NETWORK_EXPORT");
		event.setMetric("network");
		event.setValue(time);
		event.setUnits("milliseconds");
		event.setValue(time);
		NdexEventBus.INSTANCE.getEventBus().post(event);
		return network;		
	}
	
	//+++++++++++++++++ getCitationsByNetworkyId method monitoring
	// pointcut 
	pointcut getCitationsByNetworkId(String id) : execution ( 
			public Iterable<Citation> 
			org.ndexbio.task.service.NdexJVMDataModelService.getCitationsByNetworkId(String))
			&& args(id);
	//advice
	List<Citation> around (String id) : getCitationsByNetworkId(id){
		Stopwatch sw = Stopwatch.createStarted();
		List<Citation> citations = proceed(id);
		long time = sw.stop().elapsed(TimeUnit.MILLISECONDS);
		NdexExportTaskEvent event = new NdexExportTaskEvent();
		event.setOperation(thisJoinPointStaticPart.getSignature()
				.toShortString());
		event.setEntityCount(citations.size());
		event.setEntityValue("citations");
		event.setTaskType("NETWORK_EXPORT");
		event.setMetric("citations");
		event.setValue(time);
		event.setUnits("milliseconds");
		event.setValue(time);
		NdexEventBus.INSTANCE.getEventBus().post(event);
		return citations;
	
	}
	
	//+++++++++++++++++ getSubnetworkByCitationId method monitoring
	
	// pointcut 
	
	pointcut getSubnetworkByCitationId(String networkId, String citationId) : execution (
			public Network
			org.ndexbio.task.service.NdexJVMDataModelService.getSubnetworkByCitationId(String,String))
			&& args(networkId, citationId);
	// advice
	Network around (String networkId, String citationId) : getSubnetworkByCitationId(networkId, citationId){
		Stopwatch sw = Stopwatch.createStarted();
		Network network = proceed(networkId, citationId);
		long time = sw.stop().elapsed(TimeUnit.MILLISECONDS);
		NdexExportTaskEvent event = new NdexExportTaskEvent();
		event.setOperation(thisJoinPointStaticPart.getSignature()
				.toShortString());
		event.setEntityCount(network.getEdges().size());
		event.setEntityValue("edges");
		event.setTaskType("NETWORK_EXPORT");
		event.setMetric("subnetworks");
		event.setValue(time);
		event.setUnits("milliseconds");
		event.setValue(time);
		NdexEventBus.INSTANCE.getEventBus().post(event);
		return network;
	}
	
	//+++++++++++++++++ getBaseTermsByNamespace method monitoring
	//pointcut
	pointcut getBaseTermsByNamespace(String namespace,String networkId) : execution (
			public List<BaseTerm>
		org.ndexbio.task.service.NdexJVMDataModelService.getBaseTermsByNamespace(String, String) )
		&& args(namespace, networkId);
	//advice
	List<BaseTerm> around (String namespace, String networkId) :getBaseTermsByNamespace(namespace, networkId){
		Stopwatch sw = Stopwatch.createStarted();
		List<BaseTerm> baseTermList = proceed(namespace, networkId);
		long time = sw.stop().elapsed(TimeUnit.MILLISECONDS);
		NdexExportTaskEvent event = new NdexExportTaskEvent();
		event.setOperation(thisJoinPointStaticPart.getSignature()
				.toShortString());
		event.setEntityCount(baseTermList.size());
		event.setEntityValue("base terms");
		event.setTaskType("NETWORK_EXPORT");
		event.setMetric("base terms");
		event.setValue(time);
		event.setUnits("milliseconds");
		event.setValue(time);
		NdexEventBus.INSTANCE.getEventBus().post(event);
		return baseTermList;
		
	}
	
	
}
