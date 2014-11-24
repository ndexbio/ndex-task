package org.ndexbio.task.utility;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.ndexbio.common.NdexClasses;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.BaseTerm;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class XGMMLNetworkExporter {

	private NetworkDAO dao;
	
	static final private String networkTag = "graph";
	static final private String defaultNS = "http://www.cs.rpi.edu/XGMML";
	static final private String xmlns = "http://www.w3.org/2000/xmlns/";
	static final private String attTag = "att";
	static final private String nodeTag = "node";
	static final private String edgeTag = "edge";
	
	private DocumentBuilder docBuilder;
	public XGMMLNetworkExporter (ODatabaseDocumentTx db) throws ParserConfigurationException {
		dao = new NetworkDAO (db);
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docBuilder = docFactory.newDocumentBuilder();
	}

	public void exportNetwork(UUID networkId, OutputStream output) throws NdexException, 
		TransformerException, ClassCastException, SAXException, IOException {
		Network network = dao.getNetworkById(networkId);
		
		Document doc = buildXMLDocument(network);
		
	/*	

		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

		DOMImplementationLS impl =   (DOMImplementationLS)registry.getDOMImplementation("LS");


		LSSerializer writer = impl.createLSSerializer();
		LSOutput output1 = impl.createLSOutput();
		output1.setByteStream(output );
		writer.write(doc, output1);
		*/
		
		// write the content into a output stream
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(output);
		transformer.transform(source, result); 
	}
	
	private Document buildXMLDocument(Network network) throws SAXException, IOException {
		boolean isXGMMLGraph = false; 

 		// root elements
		Document doc = docBuilder.newDocument();	
		
		Element networkElement = doc.createElement(networkTag);
		doc.appendChild(networkElement);
		
		//namespaces
		for ( Namespace ns : network.getNamespaces().values()) {
			if ( ns.getPrefix().equals("xmlns")) {
				if ( ns.getUri().equals(defaultNS)) 
					isXGMMLGraph = true;
				networkElement.setAttributeNS(xmlns, ns.getPrefix(), ns.getUri());
			} else {
				networkElement.setAttributeNS(xmlns, "xmlns:"+ ns.getPrefix(), ns.getUri());		
			}
		} 
		
		Element rdfElement = addBuiltInAttributesInGraph(doc, networkElement, network.getURI()); 
		
		//network properties
		for ( NdexPropertyValuePair p : network.getProperties() ) {
		  if ( !p.getPredicateString().equals(NdexClasses.Network_P_source_format)) {	
			if ( ! p.getPredicateString().contains(":")) {
				networkElement.setAttribute(p.getPredicateString(),p.getValue());	
			} else {
				Element metaData = doc.createElement(p.getPredicateString());
				rdfElement.appendChild(metaData);
				metaData.setTextContent(p.getValue());
			}
		  }
		}

		Element title = doc.createElement("dc:title");
		rdfElement.appendChild(title);
		title.setTextContent(network.getName());
		
		Element desc = doc.createElement("dc:description");
		rdfElement.appendChild(desc);
		desc.setTextContent(network.getDescription());
		
		//network presentation property
		for ( SimplePropertyValuePair p : network.getPresentationProperties()) {
			if ( isXGMMLGraph ) {
				org.w3c.dom.Node e = getElementFromString(doc,p.getValue());
				networkElement.appendChild(e);
			} else { 
				//TODO: for non XGMML graphs, just treat them as properties.
			}	
		}
		
		//Nodes
		for (Node node : network.getNodes().values()) {
			Element nodeEle = doc.createElement(nodeTag);
			networkElement.appendChild(nodeEle);
			nodeEle.setAttribute("id", String.valueOf(node.getId()));
			nodeEle.setAttribute("name", "base"); //don't know the schema, this is the file
			if ( node.getName() != null ) 
				nodeEle.setAttribute("name", node.getName());
			
			addPropertiesToElement(node, nodeEle, doc);
		}

		for (Edge edge : network.getEdges().values()) {
			Element edgeEle = doc.createElement(edgeTag);
			networkElement.appendChild(edgeEle);
			edgeEle.setAttribute("source", String.valueOf(edge.getSubjectId()));
			edgeEle.setAttribute("target", String.valueOf(edge.getObjectId()));
			
			String pp = getBaseTermStr(network, edge.getPredicateId());
			
			String srcName = network.getNodes().get(edge.getSubjectId()).getName();
			if ( srcName == null)
				srcName = String.valueOf(edge.getObjectId());
			String targetName = network.getNodes().get(edge.getObjectId()).getName();
			if ( targetName == null)
				targetName = String.valueOf(edge.getObjectId());
			
			String ss = srcName + " (" + pp+ ") " + targetName;
			edgeEle.setAttribute("id", ss);
			edgeEle.setAttribute("label", ss);
			Element ppElt = doc.createElement(attTag);
			edgeEle.appendChild(ppElt);
			ppElt.setAttribute("label", "interaction");
			ppElt.setAttribute("name", "interaction");
			ppElt.setAttribute("value", pp);
			ppElt.setAttribute("type", "string");
/*			edgeEle.setAttribute("id", String.valueOf(node.getId()));
			if ( node.getName() != null ) 
				nodeEle.setAttribute("name", node.getName());
*/			
			addPropertiesToElement(edge, edgeEle, doc);
		} 

 
		return doc;
		
	}

	private String getBaseTermStr(Network n, long termId) {
		BaseTerm bt = n.getBaseTerms().get(termId);
		if (bt.getNamespaceId() >0 ) {
			Namespace ns = n.getNamespaces().get(bt.getNamespaceId());
			if ( ns.getPrefix()!= null)
				return ns.getPrefix() + ":" + bt.getName();
			return ns.getUri() + bt.getName();
		} 
		return bt.getName();
		
	}
	
	private void addPropertiesToElement(PropertiedObject obj, Element parent, Document doc) throws SAXException, IOException {
		for ( NdexPropertyValuePair p : obj.getProperties() ) {
			Element metaData = doc.createElement(attTag);
			parent.appendChild(metaData);
			metaData.setAttribute("label", p.getPredicateString());
			metaData.setAttribute("name", p.getPredicateString());
		    metaData.setAttribute("type", p.getDataType());
	        if ( !p.getDataType().equals("list")) { 
				metaData.setAttribute("value", p.getValue());
	        } else {
	        	
	        	String[] tokens = p.getValue().split(",(?=([^\']*\'[^\']*\')*[^\']*$)");
                for ( String v : tokens) {
                	Element valElement = doc.createElement(attTag);
                	metaData.appendChild(valElement);
                	valElement.setAttribute("value", v.substring(1, v.length()-1));
                	valElement.setAttribute("type", "string");
                }
	        }
		}
		
		for ( SimplePropertyValuePair p : obj.getPresentationProperties() ) {
		  if ( p.getName().equals("graphics") ) {
			  org.w3c.dom.Node n = getElementFromString(doc, p.getValue());
			  parent.appendChild(n);
		  } else {
			//TODO: handles other graphics property
		  }	  
		}
	}
	
	/**
	 * Add built-in elements for Graph node. 
	 * @param graph
	 * @return The "RDF" element which network metadata can be inserted under.
	 */
	private Element addBuiltInAttributesInGraph(Document doc, Element networkElement, String networkURI) {

		Element docVersion = doc.createElement(attTag);
		networkElement.appendChild(docVersion);
		docVersion.setAttribute("documentVersion", "1.0");
		
		Element metadata = doc.createElement(attTag);
		networkElement.appendChild(metadata);
		metadata.setAttribute("name", "networkMetadata");

		Element rdf = doc.createElement("rdf:RDF");
		metadata.appendChild(rdf);
		
		Element rdfDesc = doc.createElement("rdf:Description");
		rdf.appendChild(rdfDesc);
		rdfDesc.setAttribute("rdf:about", networkURI);
		return rdfDesc;
	}
	
	
	private org.w3c.dom.Node getElementFromString(Document doc, String xmlString) throws SAXException, IOException {
		Element e= this.docBuilder.parse(new ByteArrayInputStream(xmlString.getBytes())).getDocumentElement();
		org.w3c.dom.Node e2 = doc.importNode(e, true);
		
		return e2;
	}
	
	public static  void main (String[] args) throws NdexException, ParserConfigurationException, TransformerException, ClassCastException, SAXException, IOException {
		ODatabaseDocumentTx db = NdexAOrientDBConnectionPool.getInstance().acquire();
		
		XGMMLNetworkExporter exporter = new XGMMLNetworkExporter(db);
		
		FileOutputStream fo = new FileOutputStream("C:/tmp/galout.xgmml");
		exporter.exportNetwork(UUID.fromString("07e0a47f-3d78-11e4-8be6-001f3bca188f"),
				fo);
		fo.close();
	//	exporter.exportNetwork(UUID.fromString("1449182e-39f6-11e4-b298-90b11c72aefa"), System.out);
		
		db.close();

	}
	
}
