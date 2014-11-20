package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.task.Configuration;
import org.ndexbio.task.event.NdexNetworkState;
import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.task.service.NdexTaskModelService;
import org.ndexbio.task.utility.XGMMLNetworkExporter;
import org.ndexbio.xbel.exporter.XbelNetworkExporter;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import java.nio.file.Files;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

public class ImportExportTest {

	@Test
	public void test() throws Exception {

		for ( TestMeasurement m : AllTests.testList) {
		  
		  // load to db
		  IParsingEngine parser = importFile(AllTests.testFileDirectory + m.fileName, m);
			  	
		 // get the UUID of the new test network
		 UUID networkID = parser.getUUIDOfUploadedNetwork();
			
			 // verify the uploaded network
		 assertEquivalence(networkID, m);
			 
		 //export the uploaded network.
		 ODatabaseDocumentTx conn = AllTests.db.getAConnection();
	 
		 exportNetwork(m, conn, networkID);

		 if ( m.srcFormat == NetworkSourceFormat.SIF) 
			 continue;
		  
		  NetworkDAO dao = new NetworkDAO (conn);
		  dao.deleteNetwork(networkID.toString());
		  
		  // import the second round.
		  parser = importFile ( networkID.toString(), m);
		  
		  // delete exported network file
		  File file = new File(networkID.toString());
  		  file.delete();

  		  networkID = parser.getUUIDOfUploadedNetwork();
 		  assertEquivalence(networkID, m);
          
 		  //export the second round
 		  exportNetwork(m, conn, networkID);
 		  
 		  dao.deleteNetwork(networkID.toString());
 		  
 		  conn.close();

		  file = new File(networkID.toString());
 		  assertTrue( file.exists());
 		  file.delete();
		}	  
	}

	private void exportNetwork(TestMeasurement m, ODatabaseDocumentTx conn,
			UUID networkID) throws ParserConfigurationException, ClassCastException, NdexException, TransformerException, SAXException, IOException {
		  if ( m.srcFormat == NetworkSourceFormat.XGMML) {
			  XGMMLNetworkExporter exporter = new XGMMLNetworkExporter(conn);
			  FileOutputStream out = new FileOutputStream (networkID.toString());
			  exporter.exportNetwork(networkID,out);
			  out.close();
              
		  } else if ( m.srcFormat == NetworkSourceFormat.BEL) {
				NdexTaskModelService  modelService = new NdexJVMDataModelService(conn);

				// initiate the network state
				initiateStateForMonitoring(modelService, AllTests.testUser,
						networkID.toString());
				XbelNetworkExporter exporter = 
						new XbelNetworkExporter(AllTests.testUser, networkID.toString(), 
					modelService,networkID.toString());
			//
				exporter.exportNetwork();
			  
			  //  parser = new XbelParser(testFile,AllTests.testUser, AllTests.db);
		  } 

	}
	
	private IParsingEngine importFile (String fileName, TestMeasurement m) throws Exception {
		  IParsingEngine parser;	
		  String testFile = fileName;
		  if ( m.srcFormat == NetworkSourceFormat.XGMML) {
			  parser = new XgmmlParser(testFile, AllTests.testUser, 
			  			AllTests.db,m.fileName);
		  } else if ( m.srcFormat == NetworkSourceFormat.BEL) {
			  parser = new XbelParser(testFile,AllTests.testUser, AllTests.db);
		  } else if (m.srcFormat == NetworkSourceFormat.SIF) {
			  parser = new SifParser(testFile,AllTests.testUser, AllTests.db, m.fileName);
		  } else
			  throw new Exception ("unsupported source format " + m.srcFormat);
		  
		  parser.parseFile();
		  
		  return parser;

	}	
    private void assertEquivalence(UUID networkID, TestMeasurement m) throws NdexException {

    	// verify a uploaded network
		 ODatabaseDocumentTx conn = AllTests.db.getAConnection();
		 NetworkDAO dao = new NetworkDAO(conn);
		 Network n = dao.getNetworkById(networkID);
		 assertEquals(n.getName(), m.networkName);
		 assertEquals(n.getNodeCount(), n.getNodes().size());
		 assertEquals(n.getNodeCount(), m.nodeCnt);
		 assertEquals(n.getEdgeCount(), m.edgeCnt);
		 assertEquals(n.getEdges().size(), m.edgeCnt);
		 if (m.basetermCnt >=0 )
			 assertEquals(n.getBaseTerms().size(), m.basetermCnt);
		 
		 conn.close();
   
    }
    
	private static void initiateStateForMonitoring(NdexTaskModelService  modelService, 
			String userId,
			String networkId) {
		NdexNetworkState.INSTANCE.setNetworkId(networkId);
		NdexNetworkState.INSTANCE.setNetworkName(modelService.getNetworkById( networkId).getName());
		
		
	}

}
