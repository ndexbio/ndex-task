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
		URI termStringURI = new URI("http://foo.bar.org/somepath/doc#NW234");
		String scheme = termStringURI.getScheme();
		String p = termStringURI.getSchemeSpecificPart();
		String fragment= termStringURI.getFragment();
		
		System.out.println(scheme);
		System.out.println(p);
		System.out.println(fragment);
		
	}
	
/*	@Test
	public void test() throws Exception {
		SifParser parser = new SifParser("gal-filtered.sif", "Support");
		parser.parseFile();
		
		
		
	}
*/
}
