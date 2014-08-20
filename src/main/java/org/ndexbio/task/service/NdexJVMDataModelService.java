package org.ndexbio.task.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.junit.BeforeClass;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.CommonDAOValues;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;



public class NdexJVMDataModelService implements NdexTaskModelService {
	
	/*
	 * mod 16April2014 use new DAO objects from ndex-commons project
	 */
	

	 private NetworkDAO dao;
	
	 private static final Logger logger = LoggerFactory
				.getLogger(NdexJVMDataModelService.class);
	 private final int SKIP = 0;
	 private final int TOP = Integer.MAX_VALUE;
	 
	 public NdexJVMDataModelService () {
		 ODatabaseDocumentTx db = NdexAOrientDBConnectionPool.getInstance().acquire();
		 dao= new NetworkDAO (db);
	 }

	@Override
	public Network getNetworkById(String networkUUID) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkUUID), 
				"A network id is required");
	
		try {
			return dao.getNetworkById(UUID.fromString(networkUUID));
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public Collection<Citation> getCitationsByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return dao.getNetworkCitations(networkId);
		} catch (IllegalArgumentException e) {
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
	public Iterable<Namespace> getNamespacesByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");
		
		try {
			return  Iterables.filter(dao.getNetworkNamespaces(networkId), namespacePredicate);
			
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return new ArrayList<Namespace>();
	}


	@Override
	public Network getSubnetworkByCitationId(String networkId,Long citationId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return dao.getSubnetworkByCitation( networkId, citationId);
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
	public Iterable<Namespace> getInternalAnnotationsByNetworkId(
			String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		List<Namespace> internalAnnotationList = Lists.newArrayList();
		try {
			return  Iterables.filter(dao.getNetworkNamespaces(networkId), internalAnnotationPredicate);
			
			
		} catch (IllegalArgumentException e) {
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
	public Iterable<Namespace> getExternalAnnotationsByNetworkId(
			String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		List<Namespace> externalAnnotationList = Lists.newArrayList();
		try {
			return  Iterables.filter(dao.getNetworkNamespaces(networkId), externalAnnotationPredicate);
			
			
		} catch (IllegalArgumentException  e) {
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
	public Collection<BaseTerm> getBaseTermsByNamespace(String userId, String namespacePrefix,String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userId), "A userId is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(namespacePrefix), "A namespace is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), "A network id is required");
		
		try {
			return dao.getBaseTermsByPrefix( networkId, namespacePrefix);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return an empty list for error conditions
		List<BaseTerm> btList = Lists.newArrayList();
		return btList;
	}

	
	
}
