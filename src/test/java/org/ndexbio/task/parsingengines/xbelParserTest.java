package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class xbelParserTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		String user = "reactomeadmin";
		String user = "cjtest";
		XbelParser parser = new XbelParser("c:/tmp/foo2.xbel", user);
//		XbelParser parser = new XbelParser("large_corpus_unzip.xbel", user);
		parser.parseFile();
//		XbelParser 
	//	parser = new XbelParser("/home/chenjing/working/ndex/networks/selventa_full.xbel", user);
	//	parser.parseFile();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
	}

}
