package org.ndexbio.xgmml.parser.handler;

import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.IEdge;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INode;
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
			String isDirected = atts.getValue("cy:directed");
			String sourceAlias = null;
			String targetAlias = null;
			String interaction = ""; // no longer users

			if (label != null) {
				// Parse out the interaction (if this is from Cytoscape)
				// parts[0] = source alias
				// parts[1] = interaction
				// parts[2] = target alias

				final String[] parts = SPLIT.split(label);

				if (parts.length == 3) {
					sourceAlias = parts[0];
					interaction = parts[1];
					targetAlias = parts[2];
				}
			}

			final boolean directed;

			if (isDirected == null) {
				// xgmml files made by pre-3.0 cytoscape and strictly
				// upstream-XGMML conforming files
				// won't have directedness flag, in which case use the
				// graph-global directedness setting.
				//
				// (org.xml.sax.Attributes.getValue() returns null if attribute
				// does not exists)
				//
				// This is the correct way to read the edge-directionality of
				// non-cytoscape xgmml files as well.
				directed = manager.currentNetworkIsDirected;
			} else { // parse directedness flag
				directed = ObjectTypeMap.fromXGMMLBoolean(isDirected);
			}


			if (label == null || label.isEmpty())
				label = String.format("%s (%s) %s", sourceId, (directed ? "directed" : "undirected"), targetId);

			//INetwork net = manager.getCurrentNetwork();
			try {
				IEdge edge = manager.addEdge(sourceId.toString(), interaction, targetId.toString());
			} catch (ExecutionException e) {
				e.printStackTrace();
				throw new NdexException("not yet handling XLINK in XGMML");
			}


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
