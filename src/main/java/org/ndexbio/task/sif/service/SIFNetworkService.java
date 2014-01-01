package org.ndexbio.task.sif.service;

import java.util.concurrent.ExecutionException;

import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.orientdb.domain.IBaseTerm;
import org.ndexbio.orientdb.domain.ICitation;
import org.ndexbio.orientdb.domain.IEdge;
import org.ndexbio.orientdb.domain.INamespace;
import org.ndexbio.orientdb.domain.INetwork;
import org.ndexbio.orientdb.domain.INetworkMembership;
import org.ndexbio.orientdb.domain.INode;
import org.ndexbio.orientdb.domain.IUser;
import org.ndexbio.orientdb.persistence.NDExPersistenceService;
import org.ndexbio.orientdb.persistence.NDExPersistenceServiceFactory;
import org.ndexbio.rest.models.SearchParameters;
import org.ndexbio.rest.models.SearchResult;
import org.ndexbio.service.JdexIdService;
import org.ndexbio.xbel.model.*;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class SIFNetworkService {

	private static SIFNetworkService instance;

	private NDExPersistenceService persistenceService;
	private static Joiner idJoiner = Joiner.on(":").skipNulls();

	public static SIFNetworkService getInstance() {
		if (null == instance) {
			instance = new SIFNetworkService();
		}
		return instance;
	}

	private SIFNetworkService() {
		super();
		this.persistenceService = NDExPersistenceServiceFactory.INSTANCE
				.getNDExPersistenceService();
	}

	public INetwork createNewNetwork() throws Exception {
		return this.persistenceService.getCurrentNetwork();
	}

	public IUser createNewUser(String username) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(username));
		IUser user = this.persistenceService.getCurrentUser();
		user.setUsername(username);
		return user;
	}

	public INetworkMembership createNewMember() {
		return this.persistenceService.createNetworkMembership();
	}

	public SearchResult<IUser> findUsers(SearchParameters searchParameters)
			throws NdexException {
		return this.persistenceService.findUsers(searchParameters);
	}

	public void persistNewNetwork() {
		this.persistenceService.persistNetwork();
	}

	public void rollbackCurrentTransaction() {
		this.persistenceService.abortTransaction();
	}

	public IBaseTerm findOrCreateIBaseTerm(String name, INamespace namespace, Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != name, "A name is required");
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		final IBaseTerm bt = persistenceService.findOrCreateIBaseTerm(jdexId);
		if (persisted) return bt;
		bt.setName(name);
		bt.setTermNamespace(namespace);
		bt.setJdexId(jdexId.toString());
		this.persistenceService.getCurrentNetwork().addTerm(bt);
		System.out.println("Created baseTerm " 
				+ bt.getTermNamespace().getPrefix() + " " 
				+ bt.getTermNamespace().getUri() + " "
				+ bt.getName());
		return bt;
	}


	public INamespace findOrCreateINamespace(String uri, String prefix)
			throws ExecutionException {
		String namespaceIdentifier = null;
		if (uri == null && prefix == null){
			prefix = "LOCAL";
			namespaceIdentifier = "NAMESPACE:LOCAL";
		} else if (prefix != null){
			namespaceIdentifier = idJoiner.join("NAMESPACE", prefix);
		} else if (uri != null){
			prefix = this.findPrefixForNamespaceURI(uri);
			if (prefix != null){
				namespaceIdentifier = idJoiner.join("NAMESPACE", prefix);				
			} else {
				namespaceIdentifier = idJoiner.join("NAMESPACE", uri);	
			}	
		}
		
		if (uri == null && prefix != null){
			uri = findURIForNamespacePrefix(prefix);
		}
		
		Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(
				namespaceIdentifier);
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		INamespace iNamespace = persistenceService
				.findOrCreateINamespace(jdexId);
		if (persisted) return iNamespace;

		// Not persisted, fill out blank Namespace
		iNamespace.setJdexId(jdexId.toString());
		if (prefix != null) iNamespace.setPrefix(prefix);
		if (uri != null) iNamespace.setUri(uri);
		this.persistenceService.getCurrentNetwork().addNamespace(iNamespace);
		System.out.println("Created namespace " + iNamespace.getPrefix() + " " + iNamespace.getUri());
		return iNamespace;
	}

	private String findPrefixForNamespaceURI(String uri) {
		if (uri.equals("http://biopax.org/generated/group/")) return "GROUP";
		if (uri.equals("http://identifiers.org/uniprot/")) return "UniProt";
		if (uri.equals("http://purl.org/pc2/4/")) return "PathwayCommons2";
		System.out.println("No Prefix for " + uri);
		
		return null;
	}
	
	private String findURIForNamespacePrefix(String prefix){
		if (prefix.equals("UniProt")) return "http://identifiers.org/uniprot/";
		return null;
	}

	public INode findOrCreateINode(IBaseTerm baseTerm)
			throws ExecutionException {
		Preconditions.checkArgument(null != baseTerm,
				"A IBaseTerm object is required");
		String nodeIdentifier = idJoiner.join("NODE", baseTerm.getName());
		Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(
				nodeIdentifier);
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		INode iNode = persistenceService.findOrCreateINode(jdexId);
		if (persisted) return iNode;
		iNode.setJdexId(jdexId.toString());
		iNode.setRepresents(baseTerm);
		this.persistenceService.getCurrentNetwork().addNdexNode(iNode);
		return iNode;

	}
	
	public ICitation findOrCreateICitation(String type, String identifier) throws NdexException, ExecutionException {
		String citationIdentifier = idJoiner.join("CITATION",
				type, identifier);
		
			Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache()
					.get(citationIdentifier);
			boolean persisted = persistenceService.isEntityPersisted(jdexId);
			ICitation iCitation = persistenceService
					.findOrCreateICitation(jdexId);
			if (persisted)
				return iCitation;
			iCitation.setJdexId(jdexId.toString());
			iCitation.setType(type);
			iCitation.setIdentifier(identifier);
			System.out.println("Created citation " + iCitation.getType() + ":" + iCitation.getIdentifier());
			this.persistenceService.getCurrentNetwork().addCitation(iCitation);
			return iCitation;
	}

	public IEdge createIEdge(INode subjectNode, INode objectNode,
			IBaseTerm predicate)
			throws ExecutionException {
		if (null != objectNode && null != subjectNode && null != predicate) {
			Long jdexId = JdexIdService.INSTANCE.getNextJdexId();
			IEdge edge = persistenceService.findOrCreateIEdge(jdexId);
			edge.setJdexId(jdexId.toString());
			edge.setSubject(subjectNode);
			edge.setPredicate(predicate);
			edge.setObject(objectNode);
			this.persistenceService.getCurrentNetwork().addNdexEdge(edge);
			return edge;
			//System.out.println("Created edge " + edge.getJdexId());
		} 
		return null;
	}
	
	private String getNamespaceIdentifier(INamespace namespace){
		if (namespace.getPrefix() != null ) return namespace.getPrefix();
		return namespace.getUri();
	}

	public IBaseTerm findOrCreateNodeBaseTerm(String identifier, INamespace namespace)
			throws ExecutionException {
		String jdexCacheId = idJoiner.join("BASE", identifier, getNamespaceIdentifier(namespace));
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				jdexCacheId);
		return this.findOrCreateIBaseTerm(identifier, namespace, jdexId);
	}

	public IBaseTerm findOrCreatePredicate(String identifier, INamespace namespace)
			throws ExecutionException {
		String jdexCacheId = idJoiner.join("PREDICATE", identifier, getNamespaceIdentifier(namespace));
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				jdexCacheId);
		return this.findOrCreateIBaseTerm(identifier, namespace, jdexId);
	}

	public void setFormat(String format) {
		persistenceService.getCurrentNetwork().setFormat(format);	
	}



}
