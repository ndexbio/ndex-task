package org.ndexbio.task.utility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.biopax.paxtools.controller.EditorMap;
import org.biopax.paxtools.controller.PropertyEditor;
import org.biopax.paxtools.controller.SimpleEditorMap;
import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.biopax.paxtools.model.level3.XReferrable;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.ndexbio.model.tools.PropertyHelpers;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class BioPAXNetworkExporter {

	private NetworkDAO dao;
	private BioPAXFactory bioPAXFactory;
	private BioPAXIOHandler bioPAXIOHandler;
	private Map<Long, BioPAXElement> elementIdToBioPAXElementMap;

	public BioPAXNetworkExporter (ODatabaseDocumentTx db) throws ParserConfigurationException {
		dao = new NetworkDAO (db);
		bioPAXFactory = BioPAXLevel.L3.getDefaultFactory();
		bioPAXIOHandler = new SimpleIOHandler();

	}

	public void exportNetwork(UUID networkId, OutputStream output) throws NdexException, ClassCastException, IOException {
		System.out.println("Finding network to export by " + networkId);
		Network network = dao.getNetworkById(networkId);
		if (null == network){
			throw new NdexException("No Network found by: " + networkId);
		}
		Model bioPAXModel = bioPAXFactory.createModel();
		elementIdToBioPAXElementMap = new HashMap<Long, BioPAXElement>();
		setUpModel(bioPAXModel, network);
		processCitations(bioPAXModel, network);
		processNodes(bioPAXModel, network);
		processEdges(bioPAXModel, network);
		bioPAXIOHandler.convertToOWL(bioPAXModel, output);

	}

	private void setUpModel(Model bioPAXModel, Network network){
		String xmlBase = PropertyHelpers.getNetworkPropertyValueString(network, "xmlBase");
		if (null == xmlBase){
			xmlBase = "http://www.ndexbio.org/biopax/";
		}
		bioPAXModel.setXmlBase(xmlBase);
	}

	private void processCitations(Model bioPAXModel, Network network) {
		// Each Citation object becomes a PublicationXref
		// Create the PublicationXref in the model and add it to the elementIdToBioPAXObjectMap
		for (Entry<Long, Citation> entry : network.getCitations().entrySet()){
			Long elementId = entry.getKey();
			Citation citation = entry.getValue();
			String rdfId = PropertyHelpers.getCitationPropertyValueString(network, citation, "rdfId");
			System.out.println("Citation: " + elementId + " rdfId: " + rdfId);
			PublicationXref px = bioPAXModel.addNew(PublicationXref.class, rdfId);

//			String xrefDb = pubXref.getDb();
//			String xrefId = pubXref.getId();
			String idType = citation.getIdType();
			if (idType.equalsIgnoreCase("unspecified")) idType = null;
			String identifier = citation.getIdentifier();
			if (identifier.equalsIgnoreCase("unspecified")) identifier = null;
			
			String id = null;
			String db = null;
			if (null !=  idType && idType.equalsIgnoreCase("url")){
				// Special case where the NDEx citation was stored with idType "url"
				// because the pubXref db and id were not both set but there was a url
				// The url will be recovered later from the citation properties.
				// Here, the db or id, if either is available, will be recovered from citation properties
				db = PropertyHelpers.getCitationPropertyValueString(network, citation, "db");
				id = PropertyHelpers.getCitationPropertyValueString(network, citation, "id");
			} else if (null != identifier && null != idType){
				// Standard case where the citation was stored mapping 
				// db -> idType
				// id -> identifier
				id = identifier;
				db = idType;
			} else if (null != identifier){
				// Case where citation was stored only mapping
				// id -> identifier
				id = identifier;
			}
			
			if (null != db) px.setDb(db);
			if (null != id) px.setId(id);
			

//			String xrefTitle = pubXref.getTitle();
			if (null != citation.getTitle()){
				px.setTitle(citation.getTitle());
			}

//
// TBD: handle annotations, both for storing in NDEx and re-exporting
//			Map<String, Object> annotations = pubXref.getAnnotations();
//	
// TBD: handle getXrefOf
//			Set<XReferrable> refersTo = pubXref.getXrefOf();
//
			
//			Set<String> authors = pubXref.getAuthor();
			if (null != citation.getContributors()){
				for (String author : citation.getContributors()){
					px.addAuthor(author);
				}			
			}
//			Set<String> comments = pubXref.getComment();
			for (String comment : PropertyHelpers.getCitationPropertyValueStrings(network, citation, "comment")){
				px.addComment(comment);
			}
//			Set<String> sources = pubXref.getSource();	
			for (String source : PropertyHelpers.getCitationPropertyValueStrings(network, citation, "source")){
				px.addSource(source);
			}
//			String xrefDbVersion = pubXref.getDbVersion();
			String dbVersion = PropertyHelpers.getCitationPropertyValueString(network, citation, "dbVersion");
			if (null != dbVersion) px.setDbVersion(dbVersion);

//			String xrefIdVersion = pubXref.getIdVersion();
			String idVersion = PropertyHelpers.getCitationPropertyValueString(network, citation, "idVersion");
			if (null != idVersion) px.setDbVersion(idVersion);

//			int year = pubXref.getYear();
			String yearString = PropertyHelpers.getCitationPropertyValueString(network, citation, "year");
			if (null != yearString){
				// TODO: catch error. 
				int year = Integer.parseInt(yearString);
				px.setYear(year);
			}


			this.elementIdToBioPAXElementMap.put(elementId, px);
		}
		
	}

	private void processNodes(Model bioPAXModel, Network network) throws NdexException {
		for (Entry<Long, Node> e : network.getNodes().entrySet()){
			Node node = e.getValue();
			Long nodeId = e.getKey();
			
			String bioPAXType = PropertyHelpers.getNodePropertyValueString(network, node, "ndex:bioPAXType");
			Class<? extends BioPAXElement> bioPAXClass;
			try {
				bioPAXClass = (Class<? extends BioPAXElement>) Class.forName("org.biopax.paxtools.model.level3." + bioPAXType);
			} catch (ClassNotFoundException e1) {
				e1.printStackTrace();
				throw new NdexException("Unknown BioPAX class " + bioPAXType);
			}
			BioPAXElement bpe;
			String rdfId = node.getName();
			if (bioPAXClass != null && rdfId != null){
				bpe = bioPAXModel.addNew(bioPAXClass, rdfId);
			} else {
				throw new NdexException("To process node to BioPAX eed both: bioPAXClass: " + bioPAXClass + " rdfId: " + rdfId);
			}
			
			EditorMap editorMap = SimpleEditorMap.L3;
			
			// Process the node properties 
			for (NdexPropertyValuePair pvp : node.getProperties()){
				String propertyString = pvp.getPredicateString();
				if (! propertyString.equalsIgnoreCase("rdfId")){
					String value = pvp.getValue();
					PropertyEditor editor = editorMap.getEditorForProperty(propertyString, bioPAXClass);
					if (editor != null){
						editor.setValueToBean(value, bpe);
					} else {
						System.out.println("Can't export property " + propertyString + " to element class " + bioPAXClass);
					}
				}			
			}
			
			// Process the node citations
			
			// Process the node aliases
			
			// Process the node relatedTerms
			
		}
		
	}
	
	private void processEdges(Model bioPAXModel, Network network) {
		// TODO Auto-generated method stub
		
	}
	
	// 4b91eadb-5c84-11e4-9ec0-040ccee25000

	public static  void main (String[] args) throws NdexException, ParserConfigurationException, TransformerException, ClassCastException, SAXException, IOException {
		ODatabaseDocumentTx db = NdexAOrientDBConnectionPool.getInstance().acquire();
		
		BioPAXNetworkExporter exporter = new BioPAXNetworkExporter(db);
		/*
		FileOutputStream fo = new FileOutputStream("C:/tmp/galout.xgmml");
		exporter.exportNetwork(UUID.fromString("4b91eadb-5c84-11e4-9ec0-040ccee25000"),
				fo);
		fo.close();
		*/
		exporter.exportNetwork(UUID.fromString("4b91eadb-5c84-11e4-9ec0-040ccee25000"), System.out);
		
		db.close();

	}





	
}
