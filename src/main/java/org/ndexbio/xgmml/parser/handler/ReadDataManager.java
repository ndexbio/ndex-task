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
import org.ndexbio.common.models.object.network.RawNamespace;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.common.util.TermStringType;
import org.ndexbio.common.util.TermUtilities;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.xgmml.parser.ParseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;

public class ReadDataManager {
	
	private NdexPersistenceService networkService;
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
	
	
	private List<NdexPropertyValuePair> currentProperties;
	private Long currentNodeId;
	private Long currentEdgeId;

	
//	private Network currentNetwork;
	
	// Network view format properties
	private Object networkViewId;
	private Object networkId;
	private String visualStyleName;
	private String rendererName;
	private Long currentElementId; // node/edge/network old id

	
	private static final Logger logger = LoggerFactory.getLogger(ReadDataManager.class);

	public ReadDataManager(NdexPersistenceService networkService) {

		this.networkService = networkService;
		
		init();
	}

	public void init() {
		
		viewFormat = false;
		graphCount = 0;
		graphDoneCount = 0;
		documentVersion = 0;

		currentProperties = null;
		currentNodeId = null;
		currentEdgeId = null;
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

		currentNodeId = null;
		currentEdgeId = null;
		currentProperties = null;

	}
	
	public void handleSpecialNetworkAttributes() {
		List<NdexPropertyValuePair> properties = getCurrentNetwork().getProperties();
		String title = null;
		String name = null;
		String identifier = null;
		String networkName = "unknown";
		for (NdexPropertyValuePair prop : properties){
			if (prop.getPredicateString().equalsIgnoreCase("dc:description")){
				getCurrentNetwork().setDescription(prop.getValue());
			} else if (prop.getPredicateString().equalsIgnoreCase("dc:title")){
				title = prop.getValue();
			} else if (prop.getPredicateString().equalsIgnoreCase("name")){
				name = prop.getValue();
			} else if (prop.getPredicateString().equalsIgnoreCase("dc:identifier")){
				identifier = prop.getValue();
			}
		}
		if (title != null){
			networkName = title;
		} else if (name != null){
			networkName = name;
		} else if (identifier != null){
			networkName = identifier;
		}
		
		getCurrentNetwork().setName(networkName);	

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
	 * @param element an Node or Edge
	 * @param attName The name of the attribute
	 * @param attValue The value of the attribute
	 * @throws ExecutionException 
	 */
	protected void addGraphicsAttribute(Long elementId, String attName, String attValue) throws ExecutionException {
		System.out.println("Adding graphics " + attName + " = " + attValue + " to " + elementId.toString());
		this.networkService.setElementPresentationProperty(elementId, attName, attValue);
	}
	
	public List<SimplePropertyValuePair> getGraphicsAttributes(PropertiedObject element) {
		return element.getPresentationProperties();
	}
	
	protected void addGraphicsAttributes(Long elementId, Attributes atts) throws ExecutionException {
		if (elementId != null) {
			final int attrLength = atts.getLength();

			for (int i = 0; i < attrLength; i++) {
				System.out.println("Adding graphics " + atts.getLocalName(i) + " = " + atts.getValue(i) + " to " + elementId.toString());
				this.setElementProperty(elementId, atts.getLocalName(i), atts.getValue(i));
			}
		}
	}

	protected void addNetworkGraphicsAttributes( Attributes atts) {
		final int attrLength = atts.getLength();

		ArrayList<SimplePropertyValuePair> plist = new ArrayList<SimplePropertyValuePair> ();
		for (int i = 0; i < attrLength; i++) {
			SimplePropertyValuePair p = new SimplePropertyValuePair( atts.getLocalName(i), atts.getValue(i));
			plist.add(p);
		}
		this.networkService.setNetworkProperties(null, plist);
	}
	
	protected void addNetworkGraphicsAttribute( String key, String value) {

		ArrayList<SimplePropertyValuePair> plist = new ArrayList<SimplePropertyValuePair> ();
		SimplePropertyValuePair p = new SimplePropertyValuePair( key, value);
		plist.add(p);
		this.networkService.setNetworkProperties(null, plist);
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

	protected NetworkSummary getCurrentNetwork() {
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
	
	public Long findOrCreateNodeId(String id, String name) throws ExecutionException, NdexException {
		TermStringType stype = TermUtilities.getTermType(name);
		if ( stype == TermStringType.NAME) {
			return this.networkService.findOrCreateNodeIdByExternalId(id, name);
		} 
		
		Long baseTermId = this.networkService.getBaseTermId(name);
		
		Long nodeId = this.networkService.findOrCreateNodeIdByExternalId(id, null);
        
		this.networkService.setNodeRepresentTerm(nodeId, baseTermId);
		
		return null;
	}

	public Long addEdge(String subjectId, String predicate, String objectId)
			throws ExecutionException, NdexException {
		Long subjectNodeId = this.networkService.findOrCreateNodeIdByExternalId(subjectId, null);
		Long objectNodeId  = this.networkService.findOrCreateNodeIdByExternalId(objectId, null);
		Long predicateTermId = this.networkService.getBaseTermId(predicate);
		Long edgeId = this.networkService.createEdge(subjectNodeId, objectNodeId,
				predicateTermId, null, null, null);
		this.currentEdgeId = edgeId;
		return edgeId;

	}
	
	private Long addNode(String name) throws ExecutionException, NdexException {
		TermStringType stype = TermUtilities.getTermType(name);
		if ( stype == TermStringType.NAME) {
			return this.networkService.getNodeIdByName(name);
		} 
		return this.networkService.getNodeIdByBaseTerm(name);
		
	}
	
	public Long findOrCreateBaseTerm(String name, Namespace namespace) 
			throws ExecutionException, NdexException{
		return this.networkService.getBaseTermId(namespace,name );
	}
	
	public Namespace findOrCreateNamespace(String uri, String prefix) throws ExecutionException, NdexException{
		RawNamespace rns = new RawNamespace (prefix, uri);
		
		return this.networkService.findOrCreateNamespace(rns);
	}

	private Long findOrCreateBaseTermId(String termString)
			throws ExecutionException, NdexException {
		// special case for HGNC prefix with colon
		int hgncIdIndex = termString.indexOf("HGNC:HGNC:");
		if (hgncIdIndex == 0){
				String prefix = "HGNC:HGNC";
				String identifier = termString.substring(10);
				RawNamespace rns = new RawNamespace(prefix, "http://identifiers.org/hgnc/");
				Namespace ns = this.networkService.getNamespace(rns);
				return this.networkService.getBaseTermId(prefix, identifier);
		}
		
		return this.networkService.getBaseTermId(termString);
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
	
	protected void setCurrentElementId(Long currentElementId) {
		this.currentElementId = currentElementId;
	}

	public Long getCurrentNodeId() {
		return this.currentNodeId;
	}

	public void setCurrentNodeId(Long currentNode) {
		this.currentNodeId = currentNode;
	}

	public Long getCurrentEdgeId() {
		return this.currentEdgeId;
	}
	
	protected void setElementProperty ( Long elementId, String key, String value) throws ExecutionException {
		this.networkService.setElementProperty(elementId, key, value);
	}

/*
	public IRow getCurrentRow() {
		return currentRow;
	}
	
	public void setCurrentRow(CyRow row) {
		this.currentRow = row;
	}
*/	
	protected Long getCurrentElementId() {
		return this.currentElementId;
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
