package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Test;
import org.ndexbio.common.NetworkSourceFormat;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.NetworkDAO;
import org.ndexbio.model.object.network.Network;
import org.ndexbio.task.event.NdexNetworkState;
import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.task.service.NdexTaskModelService;
import org.ndexbio.task.utility.XGMMLNetworkExporter;
import org.ndexbio.xbel.exporter.XbelNetworkExporter;
import org.xml.sax.SAXException;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;



public class ImportExportTest {

	static Logger logger = Logger.getLogger(ImportExportTest.class.getName());

	@Test
	public void test() throws Exception {

		for ( TestMeasurement m : AllTests.testList) {
		  
			
		  logger.info("Testting " +m.fileName+ "\nFirst round import start.");
		  IParsingEngine parser = importFile(AllTests.testFileDirectory + m.fileName, m);
			  	
		 // get the UUID of the new test network
		 UUID networkID = parser.getUUIDOfUploadedNetwork();
			
		 logger.info("Verifying loaded content.");
		 assertEquivalence(networkID, m);
		
		 logger.info("First round import passed. Start exporting ...");
		 
		 ODatabaseDocumentTx conn = AllTests.db.getAConnection();
		 exportNetwork(m, conn, networkID);

		 logger.info("First export done.");
		 if ( m.srcFormat == NetworkSourceFormat.SIF) { 
			 System.out.println("Ignore the rest of test for " + m.fileName);
			 continue;
		 }
		 conn.close();
		 
		 conn = AllTests.db.getAConnection();
		 logger.info("Deleting network test network " + networkID.toString() + " from db.");
		  NetworkDAO dao = new NetworkDAO (conn);
		  dao.deleteNetwork(networkID.toString());

		  conn.commit();
		  
		  logger.info("Started importing exported network.");
		  parser = importFile ( networkID.toString(), m);
		  
		  logger.info("Deleting exported network document.");
		  File file = new File(networkID.toString());
  		  file.delete();
  		  

  		logger.info("Verifying network loaded from exported file.");
  		  networkID = parser.getUUIDOfUploadedNetwork();
 		  assertEquivalence(networkID, m);
          
 		  
 		 logger.info("Exporting the re-imported file.");
 		  exportNetwork(m, conn, networkID);
 		  
 		 logger.info("All tests on " + m.fileName + " passed. Deleteing test network " + networkID.toString()); 
 		  dao.deleteNetwork(networkID.toString());
 		  conn.commit();
 		  conn.close();

 		 logger.info("Deleteing network document exported from network " + networkID.toString());
		  file = new File(networkID.toString());
 		  assertTrue( file.exists());
 		  file.delete();
 		  
 		 logger.info("All done for "+ m.fileName);
		}	  
	}

	private static void exportNetwork(TestMeasurement m, ODatabaseDocumentTx conn,
			UUID networkID) throws ParserConfigurationException, ClassCastException, NdexException, TransformerException, SAXException, IOException {
		  if ( m.srcFormat == NetworkSourceFormat.XGMML) {
			  XGMMLNetworkExporter exporter = new XGMMLNetworkExporter(conn);
			  FileOutputStream out = new FileOutputStream (networkID.toString());
			  exporter.exportNetwork(networkID,out);
			  out.close();
              
		  } else if ( m.srcFormat == NetworkSourceFormat.BEL) {
				NdexTaskModelService  modelService = new NdexJVMDataModelService(conn);

				// initiate the network state
				initiateStateForMonitoring(modelService,networkID.toString());
				XbelNetworkExporter exporter = 
						new XbelNetworkExporter(AllTests.testUser, networkID.toString(), 
					modelService,networkID.toString());
			//
				exporter.exportNetwork();
			  
			  //  parser = new XbelParser(testFile,AllTests.testUser, AllTests.db);
		  } 

	}
	
	private static IParsingEngine importFile (String fileName, TestMeasurement m) throws Exception {
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
    private static void assertEquivalence(UUID networkID, TestMeasurement m) throws NdexException {

    	// verify a uploaded network
		 try (ODatabaseDocumentTx conn = AllTests.db.getAConnection()) {
			 NetworkDAO dao = new NetworkDAO(conn);
			 Network n = dao.getNetworkById(networkID);
			 assertEquals(n.getName(), m.networkName);
			 assertEquals(n.getNodeCount(), n.getNodes().size());
			 assertEquals(n.getNodeCount(), m.nodeCnt);
			 assertEquals(n.getEdgeCount(), m.edgeCnt);
			 assertEquals(n.getEdges().size(), m.edgeCnt);
			 if (m.basetermCnt >=0 )
				 assertEquals(n.getBaseTerms().size(), m.basetermCnt);
		 
		 }
   
    }
    
	private static void initiateStateForMonitoring(NdexTaskModelService  modelService, 
			String networkId) {
		NdexNetworkState.INSTANCE.setNetworkId(networkId);
		NdexNetworkState.INSTANCE.setNetworkName(modelService.getNetworkById( networkId).getName());
		
		
	}

}
