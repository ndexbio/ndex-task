package org.ndexbio.task.utility;

import static org.junit.Assert.*;

import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.task.utility.XGMMLNetworkExporter;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class XGMMLNetworkExporterTest {

	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() throws NdexException, ParserConfigurationException, TransformerException {
		ODatabaseDocumentTx db = NdexAOrientDBConnectionPool.getInstance().acquire();
		
		XGMMLNetworkExporter exporter = new XGMMLNetworkExporter(db);
		
//		exporter.exportNetwork(UUID.fromString("1449182e-39f6-11e4-b298-90b11c72aefa"), System.out);
		
		db.close();
	}

}
