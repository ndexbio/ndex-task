package org.ndexbio.xbel.parser;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.ndexbio.common.cache.NdexIdentifierCache;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.task.parsingengines.XbelFileValidator;
import org.ndexbio.task.parsingengines.XbelFileValidator.ValidationState;
import org.ndexbio.task.service.network.XBelNetworkService;
import org.ndexbio.xbel.splitter.HeaderSplitter;
import org.ndexbio.xbel.splitter.NamespaceGroupSplitter;
import org.ndexbio.xbel.splitter.StatementGroupSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/*
 * represents a parser that can map an file conforming to the XBEL schema to
 * one or more object graphs using model classes from JAXB processing of the 
 * XBEL XSD files
 * 
 * The class requires a filename for the XML file used as input
 * The specified file is tested for validity against the XBEL schemas
 * 
 */
public class XbelFileParser {

	private final String xmlFile;
	private final ValidationState validationState;
	private JAXBContext context;
	
	private XMLReader reader;
	private NamespaceGroupSplitter nsSplitter;
	private StatementGroupSplitter sgSplitter;
	private HeaderSplitter headerSplitter;
	private String ownerName;
	
	private INetwork network;
	private XBelNetworkService networkService;
	private static final Logger logger = LoggerFactory.getLogger(XbelFileParser.class);

	public XbelFileParser(String fn, String ownerName) throws JAXBException {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A filename is required");
		/*
		 * maintain a  default user for legacy tests
		 * TODO: refactor legacy tests & remove default user
		 */
		this.setOwnerName(Objects.firstNonNull(ownerName, "jstegall"));
		this.xmlFile = new File(fn).toURI().toString();
		this.validationState = new XbelFileValidator(fn).getValidationState();
		logger.info(this.validationState.getValidationMessage());
		this.context = JAXBContext.newInstance("org.ndexbio.xbel.model");
		this.nsSplitter = new NamespaceGroupSplitter(context);
		this.sgSplitter = new StatementGroupSplitter(context);
		this.headerSplitter = new HeaderSplitter(context);
		this.initReader();
	}

	public void parseXbelFile() {
		try {
			this.processHeaderAndCreateNetwork();
			this.processNamespaces();
			this.processStatementGroups();
			// at this point we can clear the identifier cache to reduce memory requirements
			logger.info("Clearing identifier cache");
			NdexIdentifierCache.INSTANCE.accessIdentifierCache().invalidateAll();
			// persist the network domain model, commit the transaction, close database connection
			this.networkService.persistNewNetwork();
		} catch (Exception e) {
			// rollback current transaction and close the database connection
			this.networkService.rollbackCurrentTransaction();
			e.printStackTrace();
		} 
		
	}
	
	private void processHeaderAndCreateNetwork() throws Exception {
		reader.setContentHandler(headerSplitter);
		try {
			reader.parse(this.getXmlFile());
		} catch (IOException | SAXException e) {
			logger.error(e.getMessage());
			throw new Exception(e);
		}
		String networkTitle = this.headerSplitter.getHeader().getName();
		this.network = this.networkService.createNewNetwork(this.getOwnerName(), networkTitle);
		
		logger.info("New test network created for XBEL: " +network.getTitle());
	}

	private void processNamespaces() throws Exception {
		logger.info("Parsing namespaces from " + this.getXmlFile());
		reader.setContentHandler(nsSplitter);
		try {
			reader.parse(this.getXmlFile());
		} catch (IOException | SAXException e) {
			logger.error(e.getMessage());
			throw new Exception(e);
		}
	}
	
	private void processStatementGroups() throws Exception {
		logger.info("Parsing statement groups from " + this.getXmlFile());
		reader.setContentHandler(sgSplitter);
		try {
			reader.parse(this.getXmlFile());
		} catch (IOException | SAXException e) {
			logger.error(e.getMessage());
			throw new Exception(e);
		}
	}

	private void initReader() {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			this.setReader(factory.newSAXParser().getXMLReader());
		} catch (SAXException | ParserConfigurationException e) {
			logger.error(e.getMessage());
		}
	}

	public ValidationState getValidationState() {
		return this.validationState;
	}

	public XMLReader getReader() {
		return reader;
	}

	public void setReader(XMLReader reader) {
		this.reader = reader;
	}

	public String getXmlFile() {
		return xmlFile;
	}

	public String getOwnerName() {
		return ownerName;
	}

	private void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
	
	

}
