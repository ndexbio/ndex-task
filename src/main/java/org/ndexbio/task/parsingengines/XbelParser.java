package org.ndexbio.task.parsingengines;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.persistence.orientdb.NdexPersistenceService;
import org.ndexbio.task.parsingengines.XbelFileValidator.ValidationState;
import org.ndexbio.xbel.splitter.AnnotationDefinitionGroupSplitter;
import org.ndexbio.xbel.splitter.HeaderSplitter;
import org.ndexbio.xbel.splitter.NamespaceGroupSplitter;
import org.ndexbio.xbel.splitter.StatementGroupSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

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
public class XbelParser implements IParsingEngine
{
    private final String xmlFile;
    private final ValidationState validationState;
    private JAXBContext context;
    private XMLReader reader;
    private NamespaceGroupSplitter nsSplitter;
    private AnnotationDefinitionGroupSplitter adSplitter;
    private StatementGroupSplitter sgSplitter;
    private HeaderSplitter headerSplitter;
//    private INetwork network;
    private String ownerName;
    private NdexPersistenceService networkService;
    private static final Logger logger = LoggerFactory.getLogger(XbelParser.class);

    public static final String belPrefix = "BEL";
    
    public XbelParser(String fn, String ownerName) throws JAXBException, NdexException, URISyntaxException
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(fn), "A filename is required");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(ownerName),
        		"A network owner name is required");
     
        File f = new File(fn);
        if ( !f.exists()) {
        	throw new NdexException("File not found: "+ f.getAbsolutePath());
        }
        this.xmlFile = f.getAbsolutePath(); 
        this.setOwnerName(ownerName);
        this.validationState = new XbelFileValidator(this.xmlFile).getValidationState();
        logger.info(this.validationState.getValidationMessage());
        this.context = JAXBContext.newInstance("org.ndexbio.xbel.model");
        this.networkService = new NdexPersistenceService(new NdexDatabase());
        this.nsSplitter = new NamespaceGroupSplitter(context, this.networkService);
        this.adSplitter = new AnnotationDefinitionGroupSplitter(context, networkService);
        this.sgSplitter = new StatementGroupSplitter(context, this.networkService);
        this.headerSplitter = new HeaderSplitter(context);
        
        this.initReader();
    }
  
    @Override
	public void parseFile()
    {
        try
        {
            this.processHeaderAndCreateNetwork();
            this.processNamespaces();
            this.processAnnotationDefinitions();
            this.processStatementGroups();
            
            
            // set edge count and node count,
            // then close database connection
            this.networkService.persistNetwork();
        }
        catch (Exception e)
        {
            // rollback current transaction and close the database connection
            this.networkService.abortTransaction();
            e.printStackTrace();
        }

    }

    private void processHeaderAndCreateNetwork() throws Exception
    {
        reader.setContentHandler(headerSplitter);
        try
        {
            reader.parse(this.getXmlFile());
        }
        catch (IOException | SAXException e)
        {
            logger.error(e.getMessage());
            throw new Exception(e);
        }
        String networkTitle = this.headerSplitter.getHeader().getName();
        this.networkService.createNewNetwork(
        		this.getOwnerName(), 
        		networkTitle,
        		this.headerSplitter.getHeader().getVersion());
        this.networkService.setNetworkTitleAndDescription(null, this.headerSplitter.getHeader().getDescription());
//        this.networkService.setFormat("BEL_DOCUMENT");

    }

    private void processNamespaces() throws Exception
    {
        logger.info("Parsing namespaces from " + this.getXmlFile());
        reader.setContentHandler(nsSplitter);
        try
        {
            reader.parse(this.getXmlFile());
        }
        catch (IOException | SAXException e)
        {
            logger.error(e.getMessage());
            throw new Exception(e);
        }
    }
    
	private void processAnnotationDefinitions() throws Exception {
		logger.info("Parsing annotation definitions from " + this.getXmlFile());
		reader.setContentHandler(adSplitter);
		try {
			reader.parse(this.getXmlFile());
		} catch (IOException | SAXException e) {
			logger.error(e.getMessage());
			throw new Exception(e);
		}
	}

    private void processStatementGroups() throws Exception
    {
        logger.info("Parsing statement groups from " + this.getXmlFile());
        reader.setContentHandler(sgSplitter);
        try
        {
            reader.parse(this.getXmlFile());
        }
        catch (IOException | SAXException e)
        {
            logger.error(e.getMessage());
            throw new Exception(e);
        }
    }

    private void initReader()
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try
        {
            this.setReader(factory.newSAXParser().getXMLReader());
        }
        catch (SAXException | ParserConfigurationException e)
        {
            logger.error(e.getMessage());
        }
    }

    public ValidationState getValidationState()
    {
        return this.validationState;
    }

    public XMLReader getReader()
    {
        return reader;
    }

    public void setReader(XMLReader reader)
    {
        this.reader = reader;
    }

    public String getXmlFile()
    {
        return xmlFile;
    }

	public String getOwnerName() {
		return ownerName;
	}

	private void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}



}
