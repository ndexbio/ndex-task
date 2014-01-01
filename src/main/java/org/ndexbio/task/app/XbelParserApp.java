package org.ndexbio.task.app;

import org.ndexbio.xbel.parser.XbelFileParser;

/*
 * Java application to evaluate parsing a specified file in XBEL format
 */

public class XbelParserApp {
	public static void main(String[] args) throws Exception {
		String filename = null;
		if(args.length > 0 ){
			filename = args[0];
		} else {
			 //filename = "large_corpus.xbel";
			 filename = "/Users/dextergraphics/Documents/bel/xbel/large_corpus.xbel";
			// filename = "/home/fcriscuo/selventa/selventa_full.xbel";
		}
		XbelFileParser parser = new XbelFileParser(filename);
		if (parser.getValidationState().isValid()){
			parser.parseXbelFile();
			
		} else {
			System.out.println(parser.getValidationState().getValidationMessage());
		}
	
	}

}
