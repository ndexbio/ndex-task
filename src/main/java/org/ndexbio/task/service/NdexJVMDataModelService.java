package org.ndexbio.task.service;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.object.BaseTerm;
import org.ndexbio.common.models.object.Citation;
import org.ndexbio.common.models.object.Edge;
import org.ndexbio.common.models.object.Namespace;
import org.ndexbio.common.models.object.NdexDataModelService;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.rest.services.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;



public class NdexJVMDataModelService implements NdexDataModelService {
	
	/*
	 * we need a mock HttpServletRequest object to use the RESTul service operations
	 */
	 final HttpServletRequest mockRequest = EasyMock.createMock(HttpServletRequest.class);
	 final NetworkService networkService = new NetworkService(mockRequest);
	 private static final Logger logger = LoggerFactory
				.getLogger(NdexJVMDataModelService.class);
	 private final int SKIP = 0;
	 private final int TOP = Integer.MAX_VALUE;

	@Override
	public Network getNetworkById(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return networkService.getNetwork(networkId);
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public List<Citation> getCitationsByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return networkService.getCitations(networkId, SKIP, TOP);
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return new ArrayList<Citation>();
	}

	@Override
	public List<Edge> getEdgesBySupportId(String supportId) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * Predicate for filtering invalid namespaces from Namespace query
	 */
	Predicate<Namespace> namespacePredicate = new Predicate<Namespace>(){
		@Override
		public boolean apply(Namespace ns) {
			return !Strings.isNullOrEmpty(ns.getUri()) && ns.getUri().contains("namespace");
		}
		
	};
	
	/*
	 * Predicate to find xbel internal annotations from namespace query
	 */
	Predicate<Namespace> internalAnnotationPredicate = new Predicate<Namespace>() {
		@Override
		public boolean apply(Namespace ns) {
			return Strings.isNullOrEmpty(ns.getUri());
		}
		
	};
	
	/*
	 * Predicate to find xbel external annotations from namespace query
	 */
	Predicate<Namespace> externalAnnotationPredicate = new Predicate<Namespace>() {

		public boolean apply(Namespace ns) {
			return !Strings.isNullOrEmpty(ns.getUri()) && ns.getUri().contains("annotation");
		}
		
	};
	
	
	/*
	 * public method to get Namespaces for a network.
	 * Since both XBEL annotation definitions and namespaces are mapped to the
	 * NDEx Namespace vertex, this method filters out annotation definitions
	 * 
	 */
	@Override
	public List<Namespace> getNamespacesByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");
		List<Namespace> namespaceList = Lists.newArrayList();
		try {
			return (List<Namespace>) Iterables.filter(networkService.getNamespaces(networkId, 0, 1000), namespacePredicate);
			
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return new ArrayList<Namespace>();
	}


	@Override
	public Network getSubnetworkByCitationId(String networkId,
			String citationId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(citationId), 
				"A citation id is required");
		try {
			return networkService.getEdgesByCitations(networkId, SKIP, TOP, new String[] {citationId});
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}


	@Override
	/*
	 * XBEL Internal Annotations are persisted as Namespace types in the database
	 * They are distinguished from External Annotations by not having a URI
	 * 
	 */
	public List<Namespace> getInternalAnnotationsByNetworkId(
			String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		List<Namespace> internalAnnotationList = Lists.newArrayList();
		try {
			return (List<Namespace>) Iterables.filter(networkService.getNamespaces(networkId, 0, 1000), internalAnnotationPredicate);
			
			
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return internalAnnotationList;
	}


	@Override
	/*XBEL External Annotations are persisted as Namespace types in the database
	 * They are distinguished from internal annotations by having a URI property
	 * 
	 */
	public List<Namespace> getExternalAnnotationsByNetworkId(
			String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		List<Namespace> externalAnnotationList = Lists.newArrayList();
		try {
			return (List<Namespace>) Iterables.filter(networkService.getNamespaces(networkId, 0, 1000), externalAnnotationPredicate);
			
			
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return externalAnnotationList;
	}
    /*
     * return a list of BaseTerms associated with a specified namespace
     * needed to resolve internal annotations
     */
	@Override
	public List<BaseTerm> getBaseTermsByNamespace(String namespace,String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(namespace), "A namespace is required");
		
		try {
			return networkService.getTermsInNamespaces(networkId, new String[] {namespace});
		} catch (IllegalArgumentException | NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return an empty list for error conditions
		List<BaseTerm> btList = Lists.newArrayList();
		return btList;
	}

}
