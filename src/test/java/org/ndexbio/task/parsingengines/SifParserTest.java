package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class SifParserTest {

	private static ODatabaseDocumentTx db;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db = NdexAOrientDBConnectionPool.getInstance().acquire();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		db.close();
	}

	@Test
	public void test() throws Exception {
		SifParser parser = new SifParser("gal-filtered.sif", "Support");
		parser.parseFile();
		
		
		
	}

}
