package org.ndexbio.task.utility;

import java.io.ByteArrayInputStream;
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

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.PropertiedObject;
import org.ndexbio.model.object.SimplePropertyValuePair;
import org.ndexbio.model.object.network.Namespace;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.model.object.network.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class XGMMLNetworkExporter {

	private NetworkDAO dao;
	
	static final private String networkTag = "graph";
	static final private String defaultNS = "http://www.cs.rpi.edu/XGMML";
	static final private String xmlns = "http://www.w3.org/2000/xmlns/";
	static final private String attTag = "att";
	static final private String nodeTag = "node";
	
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
			if ( ! p.getPredicateString().contains(":")) {
				networkElement.setAttribute(p.getPredicateString(),p.getValue());	
			} else {
				Element metaData = doc.createElement(p.getPredicateString());
				rdfElement.appendChild(metaData);
				metaData.setTextContent(p.getValue());
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
			org.w3c.dom.Node e = getElementFromString(doc,p.getValue());
			networkElement.appendChild(e);
		}
		
		//Nodes
		for (Node node : network.getNodes().values()) {
			Element nodeEle = doc.createElement(nodeTag);
			networkElement.appendChild(nodeEle);
			nodeEle.setAttribute("id", String.valueOf(node.getId()));
			if ( node.getName() != null ) 
				nodeEle.setAttribute("name", node.getName());
			
			addPropertiesToElement(node, nodeEle, doc);
		}
		
 
		return doc;
		
	}

	
	private void addPropertiesToElement(PropertiedObject obj, Element parent, Document doc) {
		for ( NdexPropertyValuePair p : obj.getProperties() ) {
			Element metaData = doc.createElement(attTag);
			parent.appendChild(metaData);
			metaData.setAttribute("label", p.getPredicateString());
			metaData.setAttribute("name", p.getPredicateString());
			metaData.setAttribute("value", p.getValue());
			metaData.setAttribute("type", p.getDataType());
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
		
		exporter.exportNetwork(UUID.fromString("1449182e-39f6-11e4-b298-90b11c72aefa"), System.out);
		
		db.close();

	}
	
}
