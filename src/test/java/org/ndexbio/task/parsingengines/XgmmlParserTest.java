package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.task.Configuration;

public class XgmmlParserTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
    	// read configuration
    	Configuration configuration = Configuration.getInstance();
    	
    	//and initialize the db connections
    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
    			configuration.getDBURL(),
    			configuration.getDBUser(),
    			configuration.getDBPasswd());
    	
    	
		NdexDatabase db = new NdexDatabase(configuration.getHostURI());
		
		String user = "pidadmin";
		XgmmlParser parser = new XgmmlParser("C:/git/ndex-task/galFiltered.xgmml", user, 
				db);
		parser.parseFile();
//		XbelParser 
//		parser = new XbelParser("/home/chenjing/working/ndex/networks/selventa_full.xbel", user);
//		parser.parseFile();

		db.close();
		NdexAOrientDBConnectionPool.close();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		//fail("Not yet implemented");
	}

}
