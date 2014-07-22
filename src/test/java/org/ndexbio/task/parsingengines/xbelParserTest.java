package org.ndexbio.task.parsingengines;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class xbelParserTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		XbelParser parser = new XbelParser("tiny_corpus.xbel", "Support");
		parser.parseFile();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
	}

}
