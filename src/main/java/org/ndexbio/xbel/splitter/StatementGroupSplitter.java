package org.ndexbio.xbel.splitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.FunctionTerm;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.ReifiedEdgeTerm;
import org.ndexbio.model.object.network.Support;
import org.ndexbio.task.parsingengines.XbelParser;
import org.ndexbio.xbel.model.Annotation;
import org.ndexbio.xbel.model.AnnotationGroup;
import org.ndexbio.xbel.model.Citation;
import org.ndexbio.xbel.model.Parameter;
import org.ndexbio.xbel.model.Statement;
import org.ndexbio.xbel.model.StatementGroup;
import org.ndexbio.xbel.model.Subject;
import org.ndexbio.xbel.model.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class StatementGroupSplitter extends XBelSplitter {
	private static final String xmlElement = "statementGroup";

	private static final Logger logger = LoggerFactory
			.getLogger(StatementGroupSplitter.class);
	
	private NdexPersistenceService networkService;

	public StatementGroupSplitter(JAXBContext context,
			NdexPersistenceService networkService) {
		super(context, xmlElement);
		this.networkService = networkService;

	}

	@Override
	protected void process() throws JAXBException, ExecutionException,
			NdexException {
		// instantiate outer level StatementGroup
		StatementGroup sg = (StatementGroup) unmarshallerHandler.getResult();
		this.processStatementGroup(sg);

	}

	private void processStatementGroup(StatementGroup sg)
			throws ExecutionException, NdexException {
		processStatementGroup(sg, null, null, null);
	}

	// In an XBEL document, only one Citation and one Support are in scope for
	// any Statement
	// Therefore, as we recursively process StatementGroups, if the
	// AnnotationGroup for the inner StatementGroup
	// contains a Citation, it overrides any Citation set in the outer
	// StatementGroup. And the same
	// is true for Supports.
	private void processStatementGroup(StatementGroup sg,
			Support outerSupport, org.ndexbio.model.object.network.Citation outerCitation,
			Map<String, String> outerAnnotations) throws ExecutionException,
			NdexException {

		// process the Annotation group for this Statement Group
		AnnotationGroup annotationGroup = sg.getAnnotationGroup();

		Map<String, String> annotations = annotationsFromAnnotationGroup(annotationGroup);
		if (null != outerAnnotations){
			// if outerAnnotations is not null, then need to deal with them
			if (null != annotations) {
				for (String key : outerAnnotations.keySet()) {
					if (!annotations.containsKey(key)) {
						// The annotations from the outer group are carried forward
						// unless the inner group overrides them.
						annotations.put(key, outerAnnotations.get(key));
					}
				}
			} else {
				// since annotations was null, just use the outer annotations 
				annotations = outerAnnotations;
			}
		}

		org.ndexbio.model.object.network.Citation citation = citationFromAnnotationGroup(annotationGroup);
		if (citation != null) {
			// The AnnotationGroup had a Citation. This overrides the
			// outerCitation.
			// Furthermore, this means that the outerSupport does NOT apply to
			// the inner StatementGroup
			// The Support will either be null or will be specified in the
			// AnnotationGroup
			outerSupport = null;
		} else {
			// There was no Citation in the AnnotationGroup, so use the
			// outerCitation
			citation = outerCitation;
		}

		// The ICitation is passed to the supportFromAnnotationGroup method
		// because
		// any ISupport created will be in the context of the ICitation and
		// should be linked to it.
		Support support = supportFromAnnotationGroup(annotationGroup, citation);
		if (support == null) {
			// The AnnotationGroup had no Support, therefore use the
			// outerSupport
			support = outerSupport;
		}

		// process the Statements belonging to this Statement Group
		this.processStatements(sg, support, citation, annotations);
		// process any embedded StatementGroup(s)
		for (StatementGroup isg : sg.getStatementGroup()) {
			this.processStatementGroup(isg, support, citation, annotations);
		}
	}

	private Map<String, String> annotationsFromAnnotationGroup(
			AnnotationGroup annotationGroup) {
		if (null == annotationGroup)
			return null;
		Map<String,String> annotationMap = new HashMap<String, String>();
		for (Object object : annotationGroup
				.getAnnotationOrEvidenceOrCitation()) {
			if (object instanceof Annotation) {
				Annotation annotation = (Annotation)object;
				annotationMap.put(annotation.getRefID(), annotation.getValue());
			}
		}
		return annotationMap;
	}

	private Support supportFromAnnotationGroup(
			AnnotationGroup annotationGroup, org.ndexbio.model.object.network.Citation citation)
			throws ExecutionException {
		if (null == annotationGroup)
			return null;
		for (Object object : annotationGroup
				.getAnnotationOrEvidenceOrCitation()) {
			if (object instanceof String) {
				// No explicit type for Evidence, therefore if it is a string,
				// its an Evidence and we find/create an ISupport
				return this.networkService.getSupport(
						(String) object, citation.getId());
			}
		}
		return null;
	}

	private org.ndexbio.model.object.network.Citation citationFromAnnotationGroup(
			AnnotationGroup annotationGroup) throws NdexException {
		if (null == annotationGroup)
			return null;
		for (Object object : annotationGroup
				.getAnnotationOrEvidenceOrCitation()) {
			if (object instanceof Citation) {
				
				Citation c = (Citation)object;
				
				return this.networkService
						.getCitation(c.getName(), c.getType().toString(), c.getReference(), 
							(c.getAuthorGroup() == null ? null : c.getAuthorGroup().getAuthor())	
					        );
			}
		}
		return null;
	}

	/*
	 * process statement group
	 */
	private void processStatements(StatementGroup sg, Support support,
			org.ndexbio.model.object.network.Citation citation, Map<String,String> annotations) 
					throws ExecutionException, NdexException {
		List<Statement> statementList = sg.getStatement();
		for (Statement statement : statementList) {
			processStatement(statement, support, citation, annotations, 0);
		}
	}

	private Edge processStatement(Statement statement, Support support,
			org.ndexbio.model.object.network.Citation citation, Map<String,String> annotations, int level) 
					throws ExecutionException, NdexException {
		if (level > 1) throw new NdexException("Attempt to process XBEL nested statement at level greater than 1");
		if (null != statement.getSubject()) {

			// All statements are expected to have a subject.
			// It is valid to have a statement with just a subject
			// It creates a node - the biological meaning is "this exists"
			Node subjectNode = this.processStatementSubject(statement
					.getSubject());
			// A typical statement, however, has a relationship, and object
			// as well as a subject
			// In that case, we can create an edge

			if (null != statement.getRelationship()) {
				BaseTerm predicate = this.networkService.getBaseTerm(
						XbelParser.belPrefix + ":"+statement.getRelationship().name());

				Node objectNode = this.processStatementObject(statement
						.getObject(), support, citation, annotations, level);

				return this.networkService.createEdge(subjectNode, objectNode,
						predicate, support, citation, annotations);
			} 
			
			//System.out.println("Handling subject-only statement for node: " + subjectNode.getJdexId() );
			this.networkService.addMetaDataToNode(subjectNode, support, citation, annotations);
			return null;

		} 

		throw new NdexException("No subject for XBEL statement in " + statement.toString());
	}
	

	private Node processStatementSubject(Subject sub)
			throws ExecutionException, NdexException {
		if (null == sub) {
			return null;
		}
		try {
			FunctionTerm representedTerm = this.processFunctionTerm(sub
					.getTerm());
			Node subjectNode = this.networkService.
					getNodeByFunctionTerm(representedTerm);
			return subjectNode;
		} catch (ExecutionException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw (e);
		}

	}

	private Node processStatementObject(org.ndexbio.xbel.model.Object obj, 
			  Support support,
			  org.ndexbio.model.object.network.Citation citation, 
			  Map<String,String> annotations, int level)
			throws ExecutionException, NdexException {
		if (null == obj) {
			//TODO: Is this allowed? throws an exceptions? --cj
			return null;
		}
		try {
			if (null != obj.getStatement()) {
				// Case: object is another statement.
				// 
				// handled by processing the statement in the same context.
				// creates:
				//  1. the edge corresponding to the statement
				//  2. a term of type "reifiedEdgeTerm" which references the edge
				//  3. a object node which the term represents
				//
				Edge reifiedEdge = this.processStatement(obj.getStatement(), support,
						              citation, annotations, level + 1);
				ReifiedEdgeTerm representedTerm = this.networkService.getReifedEdgeTermForEdge(reifiedEdge.getId());
				
				Node objectNode = this.networkService.getNodeByReifiedEdgeTerm(representedTerm);
						
				return objectNode;
			} 
			
			FunctionTerm representedTerm = this.processFunctionTerm(obj
						.getTerm());
			Node objectNode = this.networkService.getNodeByFunctionTerm(representedTerm);
					
			return objectNode;
		} catch (ExecutionException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			throw (e);
		}
	}

	private FunctionTerm processFunctionTerm(Term term)
			throws ExecutionException, NdexException {
		// XBEL "Term" corresponds to NDEx FunctionTerm
		
		BaseTerm function = this.networkService.getBaseTerm(XbelParser.belPrefix +":"+term.getFunction());

		List<org.ndexbio.model.object.network.Term> argumentList = this.processInnerTerms(term);

		return persistFunctionTerm(function, argumentList);
	}

	/*
	 * A XBel Term model object implements a quasi-composite pattern in that it
	 * contains a List<Object> that my contain either Term or Pattern model
	 * objects. A XBel Term object is equivalent to a NDEx FunctionTerm object
	 * while a XBel Parameter object is equivalent to a NDEx BaseTerm object.
	 * 
	 * This method works through the parent/child hierarchy of the outermost
	 * Term found in a found in a XBel Subject or (XBel) Object object. It
	 * utilizes the XBelCacheService to distinguish novel from existing
	 * FunctionTerms and BaseTerms. It maintains an identifier for each
	 * FunctionTerm and BaseTerm. For FunctionTerms this is a concatenated
	 * String of the JDex IDs of its children. For BaseTerms, it is a String
	 * containing the namespace and term value.
	 */
	private List<org.ndexbio.model.object.network.Term> 
	       processInnerTerms(Term term) throws ExecutionException,
			NdexException {

		List<org.ndexbio.model.object.network.Term> argumentList = 
				new ArrayList<org.ndexbio.model.object.network.Term> ();

		for (Object item : term.getParameterOrTerm()) {
			if (item instanceof Term) {

				FunctionTerm functionTerm = processFunctionTerm((Term) item);
				argumentList.add(functionTerm);

			} else if (item instanceof Parameter) {
				Parameter parameter = (Parameter)item;
				String termString = 
				 ((parameter.getNs() == null) ? XbelParser.belPrefix : parameter.getNs()) +
				  		":" + parameter.getValue();
				BaseTerm baseTerm = this.networkService.getBaseTerm(termString);
				argumentList.add(baseTerm);
			} else {
				Preconditions.checkArgument(true,
						"unknown argument to function term " + item);
			}
		}
		return argumentList;
	}

	/*
	private Long generateFunctionTermJdexId(IBaseTerm function,
			List<String> argumentJdexList) throws ExecutionException {

		return NdexIdentifierCache.INSTANCE.accessTermCache().get(
				idJoiner.join("TERM", function.getJdexId(), argumentJdexList));
	}*/

	private FunctionTerm persistFunctionTerm( BaseTerm function,
			List<org.ndexbio.model.object.network.Term> argumentList) {
		try {
			return this.networkService.getFunctionTerm(function.getId(), argumentList);

		} catch (ExecutionException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
			return null;
		}

	}

	/*
	 * private IBaseTerm createBaseTermForFunctionTerm(Term term, List<ITerm>
	 * idList) throws ExecutionException { Parameter p = new Parameter();
	 * p.setNs("BEL"); p.setValue(term.getFunction().value());
	 * 
	 * Long jdexId = XbelCacheService.INSTANCE.accessTermCache().get(
	 * idJoiner.join(p.getNs(), p.getValue())); IBaseTerm bt =
	 * XBelNetworkService.getInstance().createIBaseTerm(p, jdexId);
	 * idList.add(bt); return bt; }
	 */
}
