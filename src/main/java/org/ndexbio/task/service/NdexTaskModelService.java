package org.ndexbio.task.service;

import java.util.List;

import org.ndexbio.common.models.object.BaseTerm;
import org.ndexbio.common.models.object.Citation;
import org.ndexbio.common.models.object.Edge;
import org.ndexbio.common.models.object.Namespace;
import org.ndexbio.common.models.object.Network;

/*
 * Represents a set of service operations to interact with NDEx model objects.
 * Implementations may utilize the REST service classes directly or through
 * HTTP-based invocation of NDEx RESTful operations
 * 
 */

public interface NdexTaskModelService { 
	public Network getNetworkById(String networkId);
	
	public List<Citation> getCitationsByNetworkId(String networkId);
	public Network getSubnetworkByCitationId(String networkId, String citationId);
	public Iterable<Edge> getEdgesBySupportId(String supportId);
	public Iterable<Namespace> getNamespacesByNetworkId(String networkId);
	// internal & external annotations are persisted as namespaces
	public Iterable<Namespace> getInternalAnnotationsByNetworkId(String networkId);
	public Iterable<Namespace> getExternalAnnotationsByNetworkId(String networkId);
	public List<BaseTerm> getBaseTermsByNamespace(String namespace, String networkId);

}
