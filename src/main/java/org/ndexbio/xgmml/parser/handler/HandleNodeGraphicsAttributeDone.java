package org.ndexbio.xgmml.parser.handler;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleNodeGraphicsAttributeDone extends AbstractHandler {

	@Override
	public ParseState handle(String namespace, String tag, String qName,
			Attributes atts, ParseState current) throws SAXException,
			NdexException, Exception {
		
		this.manager.appendCurrentNodeGraphicsString("</");
		this.manager.appendCurrentNodeGraphicsString(tag);
		this.manager.appendCurrentNodeGraphicsString(">");
		return current;
	}

}
