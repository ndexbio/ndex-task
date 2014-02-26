package org.ndexbio.xbel.splitter;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.INamespace;
import org.ndexbio.task.service.network.XBelNetworkService;
import org.ndexbio.xbel.model.AnnotationDefinitionGroup;
import org.ndexbio.xbel.model.ExternalAnnotationDefinition;
import org.ndexbio.xbel.model.InternalAnnotationDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationDefinitionGroupSplitter extends XBelSplitter {

	private static final String xmlElement = "annotationDefinitionGroup";
	private static final Logger logger = LoggerFactory
			.getLogger(AnnotationDefinitionGroupSplitter.class);

	/*
	 * Extension of XBelSplitter to parse NamespaceGroup data from an XBEL
	 * document
	 */
	public AnnotationDefinitionGroupSplitter(JAXBContext context,
			XBelNetworkService networkService) {
		super(context, networkService, xmlElement);
	}

	@Override
	/*
	 * method to process unmarshaled XBEL Internal and External Annotation Definition elements from XBEL document
	 * 
	 * For each Annotation Definition ad, we add a namespace ns to the network.
	 * ns.prefix = ad.id
	 * 
	 * We distinguish this as a special kind of namespace by setting its metadata: isAnnotationDefinition = true
	 * 
	 * For an External Annotation Definition, a URL is specified:
	 * ns.uri = ead.url
	 * 
	 * metadata: isExternalAnnotationDefinition = true
	 * 
	 * For an Internal Annotation Definition iad,
	 * 
	 * metadata: type = InternalAnnotationDefinition
	 * metadata: description = iad.getDescription();
	 * 
	 * for each String in  iad.getListAnnotation()
	 * 
	 * create an iBaseTerm in the namespace
	 * 
	 * 
	 * responsible for registering novel namespace prefixes in the identifier
	 * cache, for determining the new or existing jdex id for the namespace and
	 * for persisting new namespaces into the orientdb databases
	 * 
	 * @see org.ndexbio.xbel.splitter.XBelSplitter#process()
	 */
	protected void process() throws JAXBException {
		AnnotationDefinitionGroup annotationDefinitionGroup = (AnnotationDefinitionGroup) unmarshallerHandler
				.getResult();
		logger.info("The XBEL document has "
				+ annotationDefinitionGroup.getInternalAnnotationDefinition()
						.size() + " internal annotation definitions");

		for (InternalAnnotationDefinition internalAnnotationDefinition : annotationDefinitionGroup.getInternalAnnotationDefinition()) {

			try {
				INamespace internalAnnotationNamespace = this.networkService.findOrCreateINamespace(
						internalAnnotationDefinition.getId(), 
						null);
				internalAnnotationNamespace.setMetadata(new HashMap<String, String>());
				internalAnnotationNamespace.getMetadata().put("type", "InternalAnnotationDefinition");
				if (null != internalAnnotationDefinition.getDescription()){
					internalAnnotationNamespace.getMetadata().put("description", internalAnnotationDefinition.getDescription());
				}
				if (null != internalAnnotationDefinition.getPatternAnnotation()){
					internalAnnotationNamespace.getMetadata().put("patternAnnotation", internalAnnotationDefinition.getPatternAnnotation());
				}
				if (null != internalAnnotationDefinition.getListAnnotation()){
					for (String annotation : internalAnnotationDefinition.getListAnnotation().getListValue()){
						// Create a term in the namespace
						this.networkService.findOrCreateAnnotation(internalAnnotationNamespace, annotation);
					}
				}


			} catch (ExecutionException e) {

				logger.error(e.getMessage());
				e.printStackTrace();
			} catch (NdexException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}

		}
		logger.info("done with internal annotation definitions");
		
		logger.info("The XBEL document has "
				+ annotationDefinitionGroup.getExternalAnnotationDefinition()
						.size() + " external annotation definitions");

		for (ExternalAnnotationDefinition externalAnnotationDefinition : annotationDefinitionGroup.getExternalAnnotationDefinition()) {

			try {
				INamespace externalAnnotationNamespace = this.networkService.findOrCreateINamespace(
						externalAnnotationDefinition.getId(), 
						externalAnnotationDefinition.getUrl());
				externalAnnotationNamespace.setMetadata(new HashMap<String, String>());
				externalAnnotationNamespace.getMetadata().put("type", "ExternalAnnotationDefinition");

			} catch (ExecutionException e) {

				logger.error(e.getMessage());
				e.printStackTrace();
			} catch (NdexException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}

		}
		logger.info("done with internal annotation definitions");

	}

}
