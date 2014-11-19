package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import java.io.FileOutputStream;
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
import org.ndexbio.task.utility.XGMMLNetworkExporter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

import static org.junit.Assert.*;


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
	 
		  if ( m.srcFormat == NetworkSourceFormat.XGMML) {
			  XGMMLNetworkExporter exporter = new XGMMLNetworkExporter(conn);
			  FileOutputStream out = new FileOutputStream (networkID.toString());
			  exporter.exportNetwork(networkID,out);
			  out.close();
              
		  } else if ( m.srcFormat == NetworkSourceFormat.BEL) {
			//  parser = new XbelParser(testFile,AllTests.testUser, AllTests.db);
		  } else
			  throw new Exception ("unsupported exporting source format " + m.srcFormat);
			  	parser.parseFile();
		  
		  parser = importFile ( networkID.toString(), m);
		  networkID = parser.getUUIDOfUploadedNetwork();
 		  assertEquivalence(networkID, m);

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
}
