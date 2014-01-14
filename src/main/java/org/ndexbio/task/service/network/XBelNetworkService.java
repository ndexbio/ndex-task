package org.ndexbio.task.service.network;

import java.util.concurrent.ExecutionException;

import org.ndexbio.service.CommonNetworkService;
import org.ndexbio.common.JdexIdService;
import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.ICitation;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.IFunctionTerm;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.common.models.data.ISupport;
import org.ndexbio.xbel.model.Citation;
import org.ndexbio.xbel.model.Function;
import org.ndexbio.xbel.model.Namespace;
import org.ndexbio.xbel.model.Parameter;
import org.ndexbio.xbel.model.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

/*
 * represents a class responsible for mapping XBel model objects to new Ndex domain objects
 * 
 * The primary justification for this class is to separate the use of XBel
 * model objects from identically named NDEx model objects
 */
public class XBelNetworkService extends CommonNetworkService  {

	
	private static final Logger logger = LoggerFactory
			.getLogger(XBelNetworkService.class);

	
	private static Joiner idJoiner = Joiner.on(":").skipNulls();

	public XBelNetworkService() {
		super();	
	}


	public void networkProgressLogCheck() throws NdexException  {
		try {
			this.persistenceService.networkProgressLogCheck();
		} catch (NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			// roll back
			this.rollbackCurrentTransaction();
			throw new NdexException(e.getMessage());
		}
		
	}

	public IBaseTerm createIBaseTerm(Parameter p, Long jdexId)
			throws ExecutionException, NdexException {
		Preconditions
				.checkArgument(null != p, "A Parameter object is required");
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid jdex id is required");
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		final IBaseTerm bt = persistenceService.findOrCreateIBaseTerm(jdexId);
		if (persisted)
			return bt;

		bt.setName(p.getValue());
		// resolve INamespace reference for this parameter from cache
		bt.setTermNamespace(persistenceService.findNamespaceByPrefix(p.getNs()));
		bt.setJdexId(jdexId.toString());
		this.getCurrentNetwork().addTerm(bt);
		this.networkProgressLogCheck();
		return bt;	
	}

	/*
	 * public method to map a XBEL model namespace object to a orientdb
	 * INamespace object n.b. this method may result in a new vertex in the
	 * orientdb database being created
	 */
	
	public INamespace createINamespace(Namespace ns, Long jdexId)
			throws NdexException, ExecutionException {
		Preconditions.checkArgument(null != ns,
				"A Namespace object is required");
		Preconditions.checkArgument(null != jdexId && jdexId.longValue() > 0,
				"A valid jdex id is required");
		INamespace newNamespace;
		
			newNamespace = persistenceService.findOrCreateINamespace(jdexId);
			newNamespace.setJdexId(jdexId.toString());
			newNamespace.setPrefix(ns.getPrefix());
			newNamespace.setUri(ns.getResourceLocation());
			// connect this namespace to the current network and commit
			this.getCurrentNetwork().addNamespace(newNamespace);
			this.networkProgressLogCheck();
			return newNamespace;
	
	}
	
	public IFunctionTerm findOrCreateIFunctionTerm(Long jdexId) throws ExecutionException, NdexException{
		IFunctionTerm functionTerm = this.getPersistenceService().findOrCreateIFunctionTerm(jdexId);
		this.getCurrentNetwork().addTerm(functionTerm);
		this.networkProgressLogCheck();
		return functionTerm;
	}
	

	/*
	 * public method to map a XBEL model Citation object to a orientdb ICitation
	 * object n.b. this method may result in a new vertex in the orientdb
	 * database being created
	 */
	
	public ICitation findOrCreateICitation(Citation citation) throws NdexException, ExecutionException {
		Preconditions.checkArgument(null != citation,
				"A Citation object is required");
		String citationIdentifier = idJoiner.join("CITATION",
				citation.getName(), citation.getReference());
		
			Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache()
					.get(citationIdentifier);
			boolean persisted = persistenceService.isEntityPersisted(jdexId);
			ICitation iCitation = persistenceService
					.findOrCreateICitation(jdexId);
			if (persisted)
				return iCitation;
			iCitation.setJdexId(jdexId.toString());
			iCitation.setTitle(citation.getName());
			iCitation.setType(citation.getType().value());
			iCitation.setContributors(citation.getAuthorGroup().getAuthor());
			/*
			if (null != citation.getAuthorGroup()
					&& null != citation.getAuthorGroup().getAuthor()) {
				iCitation
						.setContributors(citation.getAuthorGroup().getAuthor());
			}
			*/
			this.getCurrentNetwork().addCitation(iCitation);
			this.networkProgressLogCheck();
			return iCitation;
		
	}

	/*
	 * public method to map a XBEL model evidence string in the context of a
	 * Citation to a orientdb ISupport object n.b. this method may result in a
	 * new vertex in the orientdb database being created
	 */
	
	public ISupport findOrCreateISupport(String evidenceString,
			ICitation iCitation) throws ExecutionException, NdexException {
		Preconditions.checkArgument(null != evidenceString,
				"An evidence string is required");
		String supportIdentifier = idJoiner.join("SUPPORT",
				iCitation.getJdexId(), (String) evidenceString);
		Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(
				supportIdentifier);
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		ISupport iSupport = persistenceService.findOrCreateISupport(jdexId);
		if (persisted)
			return iSupport;
		iSupport.setJdexId(jdexId.toString());
		iSupport.setText(evidenceString);
		if (null != iCitation) {
			iSupport.setSupportCitation(iCitation);
		}
		this.getCurrentNetwork().addSupport(iSupport);
		this.networkProgressLogCheck();
		return iSupport;
	}

	
	public void createIEdge(INode subjectNode, INode objectNode,
			IBaseTerm predicate, ISupport support, ICitation citation)
			throws ExecutionException, NdexException {
		if (null != objectNode && null != subjectNode && null != predicate) {
			Long jdexId = JdexIdService.INSTANCE.getNextJdexId();
			IEdge edge = persistenceService.findOrCreateIEdge(jdexId);
			edge.setJdexId(jdexId.toString());
			edge.setSubject(subjectNode);
			edge.setPredicate(predicate);
			edge.setObject(objectNode);
			if (null != support) {
				edge.addSupport(support);
			}
			if (null != citation) {
				edge.addCitation(citation);			
			}
			this.getCurrentNetwork().addNdexEdge(edge);
			this.networkProgressLogCheck();
		}
	}

	/*
	 * public method to map a XBEL model Parameter object to a orientdb
	 * IBaseTerm object n.b. this method creates a vertex in the orientdb
	 * database
	 */
	
	public IBaseTerm findOrCreateParameter(Parameter parameter)
			throws ExecutionException, NdexException {
		if (null == parameter.getNs())
			parameter.setNs("BEL");
		String identifier = idJoiner.join("BASE", parameter.getNs(),
				parameter.getValue());
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				identifier);
		return this.createIBaseTerm(parameter, jdexId);
	}

	
	public IBaseTerm findOrCreatePredicate(Relationship relationship)
			throws ExecutionException, NdexException {
		Parameter parameter = new Parameter();
		parameter.setNs("BEL");
		parameter.setValue(relationship.name());
		String identifier = idJoiner.join("BASE", parameter.getNs(),
				parameter.getValue());
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				identifier);
		return this.createIBaseTerm(parameter, jdexId);
	}

	
	public IBaseTerm findOrCreateFunction(Function function)
			throws ExecutionException, NdexException {
		Parameter parameter = new Parameter();
		parameter.setNs("BEL");
		parameter.setValue(function.name());
		String identifier = idJoiner.join("BASE", parameter.getNs(),
				parameter.getValue());
		Long jdexId = NdexIdentifierCache.INSTANCE.accessTermCache().get(
				identifier);
		return this.createIBaseTerm(parameter, jdexId);
	}

	
	public INode findOrCreateINodeForIFunctionTerm(IFunctionTerm representedTerm)
			throws ExecutionException, NdexException {
		String nodeIdentifier = idJoiner.join("NODE",
				representedTerm.getJdexId());
		Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache().get(
				nodeIdentifier);
		boolean persisted = persistenceService.isEntityPersisted(jdexId);
		INode iNode = persistenceService.findOrCreateINode(jdexId);
		if (persisted)
			return iNode;
		iNode.setJdexId(jdexId.toString());
		iNode.setRepresents(representedTerm);
		this.getCurrentNetwork().addNdexNode(iNode);
		this.networkProgressLogCheck();
		return iNode;
	}
	
	public boolean isEntityPersisted(Long jdexId){
		return this.getPersistenceService().isEntityPersisted(jdexId);
	}
	
	

	
}
