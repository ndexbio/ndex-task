package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class xbelParserTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		XbelParser parser = new XbelParser("small_corpus.xbel", "Support");
		XbelParser parser = new XbelParser("/home/chenjing/working/ndex/networks/selventa_full.xbel", "Support");
		parser.parseFile();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
	}

}
