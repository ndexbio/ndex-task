package org.ndexbio.xbel.splitter;

import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.task.service.network.XBelNetworkService;
import org.ndexbio.xbel.model.Namespace;
import org.ndexbio.xbel.model.NamespaceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceGroupSplitter extends XBelSplitter {

	private static final String xmlElement = "namespaceGroup";
	private static final Logger logger = LoggerFactory
			.getLogger(NamespaceGroupSplitter.class);

	/*
	 * Extension of XBelSplitter to parse NamespaceGroup data from an XBEL
	 * document
	 */
	public NamespaceGroupSplitter(JAXBContext context,
			XBelNetworkService networkService) {
		super(context, networkService, xmlElement);
	}

	@Override
	/*
	 * method to process unmarshaled XBEL namespace elements from XBEL document
	 * responsible for registering novel namespace prefixes in the identifier
	 * cache, for determining the new or existing jdex id for the namespace and
	 * for persisting new namespaces into the orientdb databases
	 * 
	 * @see org.ndexbio.xbel.splitter.XBelSplitter#process()
	 */
	protected void process() throws JAXBException {
		NamespaceGroup ng = (NamespaceGroup) unmarshallerHandler.getResult();
		logger.info("The XBEL document has " + ng.getNamespace().size()
				+ " namespaces");

		// create BEL namespace
		Namespace belNamespace = new Namespace();
		belNamespace.setPrefix("BEL");
		belNamespace
				.setResourceLocation("http://belframework.org/schema/1.0/xbel");

		try {
			this.networkService.findOrCreateINamespace(belNamespace);
		} catch (ExecutionException e1) {
			logger.error(e1.getMessage());
			e1.printStackTrace();
		} catch (NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}

		for (Namespace ns : ng.getNamespace()) {

			try {
				this.networkService.findOrCreateINamespace(ns);

			} catch (ExecutionException e) {

				logger.error(e.getMessage());
				e.printStackTrace();
			} catch (NdexException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}

		}
		logger.info("done with namespaces");

	}

}
