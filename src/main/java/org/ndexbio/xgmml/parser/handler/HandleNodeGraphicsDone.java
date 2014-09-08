package org.ndexbio.xgmml.parser.handler;

import java.util.concurrent.ExecutionException;

import org.ndexbio.xgmml.parser.HandlerFactory;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class HandleNodeGraphicsDone extends AbstractHandler {

	
	@Override
	public ParseState handle(final String namespace, final String tag, final String qName,  Attributes atts, ParseState current) throws SAXException, ExecutionException {

		this.manager.appendCurrentNodeGraphicsString("</");
		this.manager.appendCurrentNodeGraphicsString(tag);
		this.manager.appendCurrentNodeGraphicsString(">\n");
		
		String presentationPropStr = this.manager.getCurrentNodeGraphicsString();
		Long nodeId = this.manager.getCurrentNodeId();
				this.manager.addGraphicsAttribute(nodeId, HandlerFactory.graphics, presentationPropStr);
		
		return current;
	}
}
