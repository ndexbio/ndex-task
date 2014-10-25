package org.ndexbio.task.utility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.biopax.paxtools.model.BioPAXElement;
import org.biopax.paxtools.model.BioPAXFactory;
import org.biopax.paxtools.model.BioPAXLevel;
import org.biopax.paxtools.model.Model;
import org.biopax.paxtools.model.level3.PublicationXref;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.Citation;
import org.ndexbio.model.object.network.Network;
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
			if (null != citation.getTitle()){
				px.setTitle(citation.getTitle());
			}
			this.elementIdToBioPAXElementMap.put(elementId, px);
		}
		
	}

	private void processNodes(Model bioPAXModel, Network network) {
		// TODO Auto-generated method stub
		
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
