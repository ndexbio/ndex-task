package org.ndexbio.xgmml.parser.handler;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.model.object.network.Edge;
import org.ndexbio.xgmml.parser.ObjectTypeMap;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleEdge extends AbstractHandler {

	private static final String SPLIT_PATTERN = "[()]";
	private static final Pattern SPLIT = Pattern.compile(SPLIT_PATTERN);

	@Override
	public ParseState handle(final String namespace, final String tag, final String qName, final Attributes atts, final ParseState current) throws SAXException, NdexException {
		// Get the label, id, source and target
		Object id = null;
		String href = atts.getValue(ReadDataManager.XLINK, "href");

		if (href == null) {
			// Create the edge:
			id = getId(atts);
			String label = getLabel(atts);
			Object sourceId = asLongOrString(atts.getValue("source"));
			Object targetId = asLongOrString(atts.getValue("target"));
	//Long edgeId = 
			manager.addEdge(sourceId.toString(), /*interaction,*/ targetId.toString());
//	manager.setCurrentElementId(edgeId);


		} else {
			// The edge might not have been created yet!
			// Save the reference so it can be added to the network after the
			// whole graph is parsed.
			throw new NdexException("not yet handling XLINK in XGMML");
		}

		return current;
	}

	private final Object asLongOrString(final String value) {
		if (value != null) {
			try {
				return Long.valueOf(value.trim());
			} catch (NumberFormatException nfe) {
				// TODO: warning?
			}
		}
		return value;
	}
}
