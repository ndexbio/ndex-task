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
			 filename = "tiny_corpus.xbel";
		}
		XbelFileParser parser = new XbelFileParser(filename);
		if (parser.getValidationState().isValid()){
			parser.parseXbelFile();
			
		} else {
			System.out.println(parser.getValidationState().getValidationMessage());
		}
	
	}

}
