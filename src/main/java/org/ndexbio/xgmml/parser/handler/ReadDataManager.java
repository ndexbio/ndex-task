package org.ndexbio.xgmml.parser.handler;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IBaseTerm;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.IMetadataObject;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.task.service.network.SIFNetworkService;
import org.ndexbio.xgmml.parser.ParseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

public class ReadDataManager {
	
	private SIFNetworkService networkService;
	private String ownerName;
	private String networkTitle;

	protected final static String XLINK = "http://www.w3.org/1999/xlink";
	
	private static final String PATTERN2X = "type|fill|w|h|size|width|outline|"
			 + "(cy:)?((node|edge)Transparency|(node|edge)LabelFont|(border|edge)LineType)|"
			 + "(cy:)?(source|target)Arrow(Color)?";
	private static final String BG_COLOR_PATTERN = "backgroundColor";
	
	private static final Pattern P2X = Pattern.compile(PATTERN2X);
	private static final Pattern PBG_COLOR = Pattern.compile(BG_COLOR_PATTERN);

	private String currentCData; 
	
	/* Stack of original network IDs */
	private Stack<Object> networkStack;

	/* Attribute values */
	protected ParseState attState = ParseState.NONE;
	protected String currentAttributeID;

	/** Edge handle list */
	protected List<String> handleList;
	/** X handle */
	protected String edgeBendX;
	/** Y handle */
	protected String edgeBendY;

	protected List<Object> listAttrHolder;
	
	/** The graph-global directedness, which will be used as default directedness of edges. */
	protected boolean currentNetworkIsDirected = true;

	private Map<Long/*network suid*/, Map<String/*att name*/, String/*att value*/>> networkGraphics;
	private Map<Long/*node suid*/, Map<String/*att name*/, String/*att value*/>> nodeGraphics;
	private Map<Long/*edge suid*/, Map<String/*att name*/, String/*att value*/>> edgeGraphics;
	

	//private Map<CyRow, Map<String/*column name*/, String/*equation*/>> equations;
	
	
	protected int graphCount;
	protected int graphDoneCount;
	
	private boolean viewFormat;
	private double documentVersion;
	
	
	private List<String> currentList;
	
	
	private IMetadataObject currentElement;
	private INode currentNode;
	private IEdge currentEdge;

	
	private INetwork currentNetwork;
	
	// Network view format properties
	private Object networkViewId;
	private Object networkId;
	private String visualStyleName;
	private String rendererName;
	private Object currentElementId; // node/edge/network old id

	
	private static final Logger logger = LoggerFactory.getLogger(ReadDataManager.class);

	public ReadDataManager(SIFNetworkService networkService) {

		this.networkService = networkService;
		
		init();
	}

	public void init() {
		
		viewFormat = false;
		graphCount = 0;
		graphDoneCount = 0;
		documentVersion = 0;

		currentElement = null;
		currentNode = null;
		currentEdge = null;
		//parentNetwork = null;
		currentNetworkIsDirected = true;
		//currentRow = null;

		ParseState attState = ParseState.NONE;
		currentAttributeID = null;

		/* Edge handle list */
		handleList = null;

		edgeBendX = null;
		edgeBendY = null;
		
		networkStack = new Stack<Object>();
		
		// TODO: determine how these are used 
		networkGraphics = new LinkedHashMap<Long, Map<String, String>>();
		nodeGraphics = new LinkedHashMap<Long, Map<String, String>>();
		edgeGraphics = new LinkedHashMap<Long, Map<String, String>>();
		
		networkViewId = null;
		networkId = null;
		visualStyleName = null;
		rendererName = null;

	}
	
	public void dispose() {

		currentNetwork = null;
		currentNode = null;
		currentEdge = null;
		currentElement = null;

	}
	
	public double getDocumentVersion() {
		return documentVersion;
	}
	
	public void setDocumentVersion(String documentVersion) {
		this.documentVersion = XGMMLParseUtil.parseDocumentVersion(documentVersion);
	}

	/*
	 * not supporting import of cytoscape session files...
	public boolean isSessionFormat() {
		return SessionUtil.isReadingSessionFile();
	}
	*/
	
	public boolean isViewFormat() {
		return viewFormat;
	}

	public void setViewFormat(boolean viewFormat) {
		this.viewFormat = viewFormat;
	}

	// This is code from the original Cytoscape XGMML parser 
	// Not clear which feature this is related to.
	/*
	public Set<CyNetwork> getPublicNetworks() {
		return publicNetworks;
	}

	
	public ReadCache getCache() {
		return cache;
	}
	
	public SUIDUpdater getSUIDUpdater() {
		return suidUpdater;
	}
*/
	/**
	 * @param element an INode or IEdge
	 * @param attName The name of the attribute
	 * @param attValue The value of the attribute
	 */
	protected void addGraphicsAttribute(IMetadataObject element, String attName, String attValue) {
		System.out.println("Adding graphics " + attName + " = " + attValue + " to " + element.toString());
		AttributeValueUtil.setAttribute(element, attName, attValue);
	}
	

	
	public Map<String, String> getGraphicsAttributes(IMetadataObject element) {
		return element.getMetadata();
	}
	

	
	protected void addGraphicsAttributes(IMetadataObject element, Attributes atts) {
		if (element != null) {
			final int attrLength = atts.getLength();

			for (int i = 0; i < attrLength; i++) {
				System.out.println("Adding graphics " + atts.getLocalName(i) + " = " + atts.getValue(i) + " to " + element.toString());
				AttributeValueUtil.setAttribute(element, atts.getLocalName(i), atts.getValue(i));
			}
		}
	}
	
	// This is code from the original Cytoscape XGMML parser 
	// It handles the case of Cy3 view formats
	// Not supported at this time for NDEx parsing of XGMML
	 	/**
	 * Used only when reading Cy3 view format XGMML. Because there is no network yet, we use the old model Id as
	 * mapping key.
	 * @param oldModelId The original ID of the CyNode, CyEdge or CyNetwork.
	 * @param attName
	 * @param attValue
	 * @param locked
	 */
	 /*
	protected void addViewGraphicsAttribute(Object oldModelId, String attName, String attValue, boolean locked) {
		Map<Object, Map<String, String>> graphics = locked ? viewLockedGraphics : viewGraphics;
		Map<String, String> attributes = graphics.get(oldModelId);

		if (attributes == null) {
			attributes = new HashMap<String, String>();
			graphics.put(oldModelId, attributes);
		}

		attributes.put(attName, attValue);
	}
	
	protected void addViewGraphicsAttributes(Object oldModelId, Attributes atts, boolean locked) {
		if (oldModelId != null) {
			final int attrLength = atts.getLength();
			
			for (int i = 0; i < attrLength; i++)
				addViewGraphicsAttribute(oldModelId, atts.getLocalName(i), atts.getValue(i), locked);
		}
	}
	*/


	/*
	public <T extends CyIdentifiable> Map<String, String> getViewGraphicsAttributes(final Object oldId, final boolean locked) {
		return locked ? viewLockedGraphics.get(oldId) : viewGraphics.get(oldId);
	}

	public void setParentNetwork(CyRootNetwork parent) {
		this.parentNetwork = parent;
	}
	
	public CyRootNetwork getParentNetwork() {
		return this.parentNetwork;
	}
	*/

	// This is code from the original Cytoscape XGMML parser 
	// It handles the case of equations as element properties
	// Not supported at this time for NDEx parsing of XGMML
	/**
	 * Just stores all the equation strings per CyIdentifiable and column name.
	 * It does not create the real Equation objects yet.
	 * @param row The network/node/edge row
	 * @param columnName The name of the column
	 * @param formula The equation formula
	 */
	/*
	public void addEquationString(CyRow row, String columnName, String formula) {
		Map<String, String> colEquationMap = equations.get(row);
		
		if (colEquationMap == null) {
			colEquationMap = new HashMap<String, String>();
			equations.put(row, colEquationMap);
		}
		
		colEquationMap.put(columnName, formula);
	}
	
	
	*/
	/**
	 * Should be called only after all XGMML attributes have been read.
	 */
	/*
	protected void parseAllEquations() {
		for (Map.Entry<CyRow, Map<String, String>> entry : equations.entrySet()) {
			CyRow row = entry.getKey();
			Map<String, String> colEquationMap = entry.getValue();
			
			Map<String, Class<?>> colNameTypeMap = new Hashtable<String, Class<?>>();
			Collection<CyColumn> columns = row.getTable().getColumns();
			
			for (CyColumn col : columns) {
				colNameTypeMap.put(col.getName(), col.getType());
			}
			
			for (Map.Entry<String, String> colEqEntry : colEquationMap.entrySet()) {
				String columnName = colEqEntry.getKey();
				String formula = colEqEntry.getValue();

				if (equationCompiler.compile(formula, colNameTypeMap)) {
					Equation equation = equationCompiler.getEquation();
					row.set(columnName, equation);
				} else {
					logger.error("Error parsing equation \"" + formula + "\": " + equationCompiler.getLastErrorMsg());
				}
			}
		}
	}
	*/

	protected INetwork getCurrentNetwork() {
		return this.networkService.getCurrentNetwork();
	}
	
	/**
	 * @return Stack of network IDs (XGMML IDs).
	 */
	protected Stack<Object> getNetworkIDStack() {
		return networkStack;
	}
	
	// This is code from the original Cytoscape XGMML parser 
	// It handles the case of subnetworks expressed as compound nodes 
	// Not supported at this time for NDEx parsing of XGMML

	/*
	public Stack<CyNode> getCompoundNodeStack() {
		return compoundNodeStack;
	}

	protected CyRootNetwork createRootNetwork() {
		if (this.rootNetwork != null)
			return this.rootNetwork;
		
		final CyNetwork baseNet = networkFactory.createNetwork();
		final CyRootNetwork rootNetwork = rootNetworkManager.getRootNetwork(baseNet);
		
		return rootNetwork;
	}
	
	protected CyRootNetwork getRootNetwork() {
		return (currentNetwork != null) ? rootNetworkManager.getRootNetwork(currentNetwork) : null;
	}
	*/
	
	public INode findOrCreateNode(String id, String name) throws ExecutionException {
		IBaseTerm term = null;
		if (name != null){
			term = findOrCreateBaseTerm(name);
		} else {
			term = findOrCreateBaseTerm(id);
		}
		if (null != term) {
			INode node = this.networkService.findOrCreateINode(id, term);
			return node;
		}
		return null;
	}

	public IEdge addEdge(String subject, String predicate, String object)
			throws ExecutionException, NdexException {
		INode subjectNode = this.networkService.findINode(subject);
		INode objectNode = this.networkService.findINode(object);
		IBaseTerm predicateTerm = findOrCreateBaseTerm(predicate);
		IEdge edge = this.networkService.createIEdge(subjectNode, objectNode,
				predicateTerm);
		this.setCurrentEdge(edge);
		return edge;

	}
	
	public IBaseTerm findOrCreateBaseTerm(String name, INamespace namespace) throws ExecutionException{
		return this.networkService.findOrCreateBaseTerm(name, namespace);
	}
	
	public INamespace findOrCreateNamespace(String uri, String prefix) throws ExecutionException{
		return this.networkService.findOrCreateINamespace(uri, prefix);
	}

	private IBaseTerm findBaseTerm(String termString) throws ExecutionException {
		// case 1 : termString is a URI
		// example: http://identifiers.org/uniprot/P19838
		// treat the last element in the URI as the identifier and the rest as
		// the namespace URI
		// find or create the namespace based on the URI
		// when creating, set the prefix based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		IBaseTerm iBaseTerm = null;
		try {
			URI termStringURI = new URI(termString);
			String path = termStringURI.getPath();
			if (path != null && path.indexOf("/") != -1) {
				String identifier = path.substring(path.lastIndexOf('/') + 1);
				String namespaceURI = termString.substring(0,
						termString.lastIndexOf('/') + 1);
				INamespace namespace = this.networkService.findINamespace(
						namespaceURI, null);
				iBaseTerm = this.networkService.findNodeBaseTerm(identifier,
						namespace);
				return iBaseTerm;
			}

		} catch (URISyntaxException e) {
			// ignore and move on to next case
		}

		// case 2: termString is of the form NamespacePrefix:Identifier
		// find or create the namespace based on the prefix
		// when creating, set the URI based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		String[] termStringComponents = termString.split(":");
		if (termStringComponents != null && termStringComponents.length == 2) {
			String identifier = termStringComponents[1];
			String prefix = termStringComponents[0];
			INamespace namespace = this.networkService.findINamespace(null,
					prefix);
			iBaseTerm = this.networkService.findNodeBaseTerm(identifier,
					namespace);
			return iBaseTerm;
		}

		// case 3: termString cannot be parsed, use it as the identifier.
		// find or create the namespace for prefix "LOCAL" and use that as the
		// namespace.

		iBaseTerm = this.networkService.findNodeBaseTerm(termString,
				this.networkService.findINamespace(null, "LOCAL"));

		return iBaseTerm;

	}

	private IBaseTerm findOrCreateBaseTerm(String termString)
			throws ExecutionException {
		// case 1 : termString is a URI
		// example: http://identifiers.org/uniprot/P19838
		// treat the last element in the URI as the identifier and the rest as
		// the namespace URI
		// find or create the namespace based on the URI
		// when creating, set the prefix based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		IBaseTerm iBaseTerm = null;
		try {
			URI termStringURI = new URI(termString);
			String path = termStringURI.getPath();
			if (path != null && path.indexOf("/") != -1) {
				String identifier = path.substring(path.lastIndexOf('/') + 1);
				String namespaceURI = termString.substring(0,
						termString.lastIndexOf('/') + 1);
				INamespace namespace = this.networkService
						.findOrCreateINamespace(namespaceURI, null);
				iBaseTerm = this.networkService.findOrCreateNodeBaseTerm(
						identifier, namespace);
				return iBaseTerm;
			}

		} catch (URISyntaxException e) {
			// ignore and move on to next case
		}

		// case 2: termString is of the form NamespacePrefix:Identifier
		// find or create the namespace based on the prefix
		// when creating, set the URI based on the PREFIX-URI table for known
		// namespaces, otherwise do not set.
		//
		//String[] termStringComponents = termString.split(":");
		int colonIndex = termString.indexOf(":");
		// special case for HGNC prefix with colon
		int hgncIdIndex = termString.indexOf("HGNC:HGNC:");
		if (colonIndex != -1 && termString.length() > colonIndex + 2 || hgncIdIndex == 0) {
			String identifier = null;
			String prefix = null;
			if (hgncIdIndex == 0){
				prefix = "HGNC:HGNC";
				identifier = termString.substring(10);
			} else {
				identifier = termString.substring(colonIndex + 1);
				prefix = termString.substring(0, colonIndex);
			}
			
			INamespace namespace = this.networkService.findOrCreateINamespace(
					null, prefix);
			iBaseTerm = this.networkService.findOrCreateNodeBaseTerm(
					identifier, namespace);
			return iBaseTerm;
		}

		// case 3: termString cannot be parsed, use it as the identifier.
		// find or create the namespace for prefix "LOCAL" and use that as the
		// namespace.

		iBaseTerm = this.networkService.findOrCreateNodeBaseTerm(termString,
				this.networkService.findOrCreateINamespace(null, "LOCAL"));

		return iBaseTerm;
	}


	
	public Object getNetworkViewId() {
		return networkViewId;
	}

	protected void setNetworkViewId(Object networkViewId) {
		this.networkViewId = networkViewId;
	}

	public Object getNetworkId() {
		return networkId;
	}

	protected void setNetworkId(Object networkId) {
		this.networkId = networkId;
	}

	public String getVisualStyleName() {
		return visualStyleName;
	}

	protected void setVisualStyleName(String visualStyleName) {
		this.visualStyleName = visualStyleName;
	}

	public String getRendererName() {
		return rendererName;
	}

	protected void setRendererName(String rendererName) {
		this.rendererName = rendererName;
	}
	
	protected Object getCurrentElementId() {
		return currentElementId;
	}

	protected void setCurrentElementId(Object currentElementId) {
		this.currentElementId = currentElementId;
	}

	public INode getCurrentNode() {
		return currentNode;
	}

	public void setCurrentNode(INode currentNode) {
		this.currentNode = currentNode;
	}

	public IEdge getCurrentEdge() {
		return currentEdge;
	}

	public void setCurrentEdge(IEdge currentEdge) {
		this.currentEdge = currentEdge;
	}
/*
	public IRow getCurrentRow() {
		return currentRow;
	}
	
	public void setCurrentRow(CyRow row) {
		this.currentRow = row;
	}
*/	
	protected IMetadataObject getCurrentElement() {
		return currentElement;
	}
	
	public void setCurrentElement(IMetadataObject entry) {
		this.currentElement = entry;
	}

	public List<String> getCurrentList() {
		return currentList;
	}

	public void setCurrentList(List<String> currentList) {
		this.currentList = currentList;
	}
	
	public String getCurrentCData() {
		return currentCData;
	}

	public void setCurrentCData(String currentCData) {
		this.currentCData = currentCData;
	}	

 	/**
 	 * Adds old->new SUID references to the SUID Updater 
 	 * @throws Exception 
 	 */
	/*
	private void mapSUIDs(final Object oldId, final Long newSUID) {
        if (oldId instanceof Long) // if String (probably Cy2), it has to be handled differently
        	suidUpdater.addSUIDMapping((Long)oldId, newSUID);
	}
	
	*/


	/**
	 * It controls which graphics attributes should be parsed.
	 * @param element The network, node or edge
	 * @param attName The name of the XGMML attribute
	 * @return
	 */
	/*
	private final boolean ignoreGraphicsAttribute(final CyIdentifiable element, final String attName) {
		boolean b = false;
		
		// When reading XGMML as part of a CYS file, these graphics attributes should not be parsed.
		if (isSessionFormat() && element != null && attName != null) {
			// Network
			b = b || (element instanceof CyNetwork && matchesBg(attName));
			// Nodes or Edges (these are standard XGMML and 2.x <graphics> attributes, not 3.0 bypass properties)
			b = b ||
				((element instanceof CyNode || element instanceof CyEdge) && matches2x(attName));
		}

		return b;
	}
	
	private final boolean matches2x(final String text) {
		final Matcher matcher = P2X.matcher(text);
		return matcher.matches();
	}
	
	private final boolean matchesBg(final String text) {
		final Matcher matcher = PBG_COLOR.matcher(text);
		return matcher.matches();
	}
	*/

	/*
	// The following is added to support the user option to import network into different collection
	private  Map<Object, CyNode> nMap;
	private CyRootNetwork rootNetwork = null;

	public void setNodeMap(Map<Object, CyNode> nMap){
		this.nMap = nMap;
	}
	*/
}
