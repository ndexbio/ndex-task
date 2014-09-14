package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class XgmmlParserTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		String user = "pidadmin";
		XgmmlParser parser = new XgmmlParser("C:/git/ndex-task/galFiltered.xgmml", user);
		parser.parseFile();
//		XbelParser 
//		parser = new XbelParser("/home/chenjing/working/ndex/networks/selventa_full.xbel", user);
//		parser.parseFile();

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		//fail("Not yet implemented");
	}

}
