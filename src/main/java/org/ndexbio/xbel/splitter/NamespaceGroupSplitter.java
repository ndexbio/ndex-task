package org.ndexbio.xbel.splitter;

import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.orientdb.domain.INamespace;
import org.ndexbio.orientdb.persistence.NDExPersistenceService;
import org.ndexbio.orientdb.persistence.NDExPersistenceServiceFactory;
import org.ndexbio.xbel.model.Namespace;
import org.ndexbio.xbel.model.NamespaceGroup;
import org.ndexbio.xbel.service.XBelNetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceGroupSplitter extends XBelSplitter {
	
	private static final String xmlElement = "namespaceGroup";
	private static final Logger logger = LoggerFactory.getLogger(NamespaceGroupSplitter.class);
 /*
  * Extension of XBelSplitter to parse NamespaceGroup data from an XBEL document
  */
	public NamespaceGroupSplitter(JAXBContext context) {
		super(context, xmlElement);
	}
	@Override
	/*
	 * method to process unmarshaled  XBEL namespace elements from XBEL document
	 * responsible for registering novel namespace prefixes in the identifier cache,
	 * for determining the new or existing jdex id for the namespace and for persisting
	 * new namespaces into the orientdb databases
	 * 
	 * @see org.ndexbio.xbel.splitter.XBelSplitter#process()
	 */
	protected void process() throws JAXBException {
		NamespaceGroup ng = (NamespaceGroup) unmarshallerHandler
				.getResult();
		 logger.info("The XBEL document has "  +ng.getNamespace().size() 
		            +" namespaces");
		 NDExPersistenceService persistenceService = NDExPersistenceServiceFactory.
				 INSTANCE.getNDExPersistenceService();
		 	// create BEL namespace
		    Namespace bel = new Namespace();
		    bel.setPrefix("BEL");
		    bel.setResourceLocation("XYZ");
		    Long jdex;
			try {
				jdex = NdexIdentifierCache.INSTANCE.accessIdentifierCache()
						.get(bel.getPrefix());
				INamespace ibel = XBelNetworkService.getInstance().createINamespace(bel, jdex);
			} catch (ExecutionException e1) {
				logger.error(e1.getMessage());
				e1.printStackTrace();
			} catch (NdexException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}	
		   
		        for( Namespace ns : ng.getNamespace()){
		        	
		        	try {
		        		// get a existing or new JDEXid from cache
						Long jdexId = NdexIdentifierCache.INSTANCE.accessIdentifierCache()
							.get(ns.getPrefix());
						// create a INamespace instance using data from the Namespace model object	
						// n.b. this method may create a VertexFrame in the orientdb database
						INamespace ins = XBelNetworkService.getInstance().createINamespace(ns, jdexId);
						
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
