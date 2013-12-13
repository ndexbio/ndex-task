package org.ndexbio.task.app;

import org.ndexbio.task.sif.SIFFileParser;

/*
 * Java application to evaluate parsing a specified file in XBEL format
 */

public class SIFParserApp {
	public static void main(String[] args) throws Exception {
		String filename = null;
		if (args.length > 0) {
			filename = args[0];
		} else {
			filename = "galFiltered.sif";
		}
		SIFFileParser parser = new SIFFileParser(filename);
		parser.parseSIFFile();
		for (String msg : parser.getMsgBuffer()) {
			System.out.println(msg);
			/*
			 * if (parser.getValidationState().isValid()){ parser.parseFile();
			 * for (String msg : parser.getMsgBuffer()){
			 * System.out.println(msg); } } else {
			 * System.out.println(parser.getValidationState
			 * ().getValidationMessage()); }
			 */
		}
	}

}
