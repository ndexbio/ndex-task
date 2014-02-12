package org.ndexbio.xgmml.parser.handler;

/*
 * #%L
 * Cytoscape IO Impl (io-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


import java.util.concurrent.ExecutionException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INode;
import org.ndexbio.xgmml.parser.ParseState;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class HandleNode extends AbstractHandler {

	@Override
	public ParseState handle(final String tag, final Attributes atts, final ParseState current) throws SAXException, NdexException, ExecutionException {
		final String href = atts.getValue(ReadDataManager.XLINK, "href");
		Object id = null;
		String label = null;
		INode node = null;
		final INetwork curNet = manager.getCurrentNetwork();
		//final CyNetwork rootNet = manager.getRootNetwork();
		
		if (href == null) {
			
			id = getId(atts);
			// Create the node
			node = manager.findOrCreateNode(id.toString());
			label = atts.getValue("label");
			
			if (label == null)
				label = atts.getValue("name"); // For backwards compatibility
			
			node.setName(label);

			
		} else {
			throw new NdexException("Not yet handling XLINKs");
		}
		
		if (node != null){
			manager.setCurrentElement(node);
			manager.setCurrentNode(node);
		}
		
		return current;
	}
}
