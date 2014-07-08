package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;

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
	public void URITest () throws URISyntaxException {
		URI termStringURI = new URI("http://www.foo.bar.org/testpath/something#NW223");
		String scheme = termStringURI.getScheme();
		System.out.println(scheme);
		String p = termStringURI.getSchemeSpecificPart();
		System.out.println(p);
		String f = termStringURI.getFragment();
		System.out.println(f);
		
	}

/*	@Test
	public void test() throws Exception {
		SifParser parser = new SifParser("gal-filtered.sif", "Support");
		parser.parseFile();
		
		
		
	} */

}
