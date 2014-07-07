package org.ndexbio.task.service.network;

import java.util.concurrent.ExecutionException;

import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.JdexIdService;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.service.CommonNetworkService;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/*
 * Subclass of CommonNetworkService implements database operations specific to 
 * SIF-formatted files
 */

public class SIFNetworkService extends CommonNetworkService {

	private static Joiner idJoiner = Joiner.on(":").skipNulls();

	

	public SIFNetworkService() throws NdexException {
		super();
		
	}

/*	public IBaseTerm findOrCreateIBaseTerm(String name, INamespace namespace, Long jdexId)
			throws ExecutionException {
		Preconditions.checkArgument(null != name, "A name is required");
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		final IBaseTerm bt = persistenceService.findOrCreateIBaseTerm(jdexId);
		if (persisted) return bt;
		bt.setName(name);
		bt.setTermNamespace(namespace);
		bt.setJdexId(jdexId.toString());
		this.persistenceService.getCurrentNetwork().addTerm(bt);
		
		logger.info("Created baseTerm " 
				+ bt.getTermNamespace().getPrefix() + " " 
				+ bt.getTermNamespace().getUri() + " "
				+ bt.getName());
		
		return bt;
	}
	*/
/*
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
//		this.persistenceService.getCurrentNetwork().addNdexNode(iNode);
		return iNode;

	}
	*/
/*	
	public INode findOrCreateINode(String id, IBaseTerm baseTerm)
			throws ExecutionException {
		Preconditions.checkArgument(null != baseTerm,
				"A IBaseTerm object is required");
		String nodeIdentifier = idJoiner.join("NODE", id);
		Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(
				nodeIdentifier);
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		INode iNode = persistenceService.findOrCreateINode(jdexId);
		if (persisted) return iNode;
		iNode.setJdexId(jdexId.toString());
		iNode.setRepresents(baseTerm);
	//	this.persistenceService.getCurrentNetwork().addNdexNode(iNode);
		return iNode;

	}
	
	public INode findINode(String id) throws NdexException, ExecutionException {
		String nodeIdentifier = idJoiner.join("NODE", id);
		Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(
				nodeIdentifier);
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		INode iNode = persistenceService.findOrCreateINode(jdexId);
		if (persisted) return iNode;
		throw new NdexException("no INode found for id = " + id);
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
			//logger.info("Created citation " + iCitation.getType() + ":" + iCitation.getIdentifier());
//			this.persistenceService.getCurrentNetwork().addCitation(iCitation);
			return iCitation;
	}
*/

/*	public IEdge createIEdge(INode subjectNode, INode objectNode,
			IBaseTerm predicate)
			throws ExecutionException {
		if (null != objectNode && null != subjectNode && null != predicate) {
			Long jdexId = JdexIdService.INSTANCE.getNextJdexId();
			IEdge edge = persistenceService.findOrCreateIEdge(jdexId);
			edge.setJdexId(jdexId.toString());
			edge.setSubject(subjectNode);
			edge.setPredicate(predicate);
			edge.setObject(objectNode);
//			this.persistenceService.getCurrentNetwork().addNdexEdge(edge);
			return edge;
			//System.out.println("Created edge " + edge.getJdexId());
		} 
		return null;
	}
*/	
	private String getNamespaceIdentifier(Namespace namespace){
		if (namespace.getPrefix() != null ) return namespace.getPrefix();
		return namespace.getUri();
	}
/*	
	// This is called to create terms used in metadata annotations
	public IBaseTerm findOrCreateBaseTerm(String identifier, INamespace namespace)
			throws ExecutionException {
		String jdexCacheId = idJoiner.join("BASE", identifier, getNamespaceIdentifier(namespace));
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				jdexCacheId);
		return this.findOrCreateIBaseTerm(identifier, namespace, jdexId);
	}
	
	// This is called to create terms referenced by nodes
	public IBaseTerm findOrCreateNodeBaseTerm(String identifier, INamespace namespace)
			throws ExecutionException {
		String jdexCacheId = idJoiner.join("BASE", identifier, getNamespaceIdentifier(namespace));
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				jdexCacheId);
		return this.findOrCreateIBaseTerm(identifier, namespace, jdexId);
	}
	
	public IBaseTerm findNodeBaseTerm(String identifier, INamespace namespace)
			throws ExecutionException {
		String jdexCacheId = idJoiner.join("BASE", identifier, getNamespaceIdentifier(namespace));
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				jdexCacheId);
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		if (persisted) return persistenceService.findOrCreateIBaseTerm(jdexId);
		return null;
	}

	// This is called to create terms that are referenced by edges as predicates
	public IBaseTerm findOrCreatePredicate(String identifier, INamespace namespace)
			throws ExecutionException {
		String jdexCacheId = idJoiner.join("PREDICATE", identifier, getNamespaceIdentifier(namespace));
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				jdexCacheId);
		return this.findOrCreateIBaseTerm(identifier, namespace, jdexId);
	}
*/






}
