package org.ndexbio.xgmml.parser.handler;

import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class HandleNodeGraphicsDone extends AbstractHandler {

	
	@Override
	public ParseState handle(final String namespace, final String tag, final String qName,  Attributes atts, ParseState current) throws SAXException {

		/*
		if (!manager.getCompoundNodeStack().empty()) {
			CyNode node = manager.getCompoundNodeStack().pop();
			manager.setCurrentElement(node);
		}
		*/
		return ParseState.NODE;
	}
}
