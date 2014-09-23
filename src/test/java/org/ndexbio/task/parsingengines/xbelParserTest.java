package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.task.Configuration;

public class xbelParserTest {

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
		
//		String user = "reactomeadmin";
		String user = "cjtest";
//		XbelParser parser = new XbelParser("/opt/ndex/exported-networks/foo.xbel", user);
//		XbelParser parser = new XbelParser("large_corpus_unzip.xbel", user);
	//	for ( int i = 0 ; i < 3; i ++ ) {
		  XbelParser parser = new XbelParser(
				  "/home/chenjing/git/ndex-task/src/test/resources/small_corpus.xbel"
				    , user, db);
		  parser.parseFile();
	//	}
//		XbelParser 
	//	parser = new XbelParser("/home/chenjing/working/ndex/networks/selventa_full.xbel", user);
	//	parser.parseFile();
		System.out.println("closing db.");
		db.close();
		NdexAOrientDBConnectionPool.close();
		System.out.println("db closed.");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
	}

}
