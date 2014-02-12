package org.ndexbio.task.parsingengines;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.task.service.network.ExcelNetworkService;
import org.ndexbio.task.service.network.SIFNetworkService;
import org.ndexbio.xgmml.parser.HandlerFactory;
import org.ndexbio.xgmml.parser.XGMMLParser;
import org.ndexbio.xgmml.parser.handler.ReadDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.ParserAdapter;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class XgmmlParser implements IParsingEngine {
    private final File xgmmlFile;
    private final String xgmmlURI;
    private String ownerName;
    private SIFNetworkService networkService;
    private XGMMLParser parser;
    private ReadDataManager readDataManager;
    private HandlerFactory handlerFactory;
    private FileInputStream xgmmlFileStream;
    private static final Logger logger = LoggerFactory.getLogger(XgmmlParser.class);

	public XgmmlParser(String fn, String ownerName) throws Exception {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A filename is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(fn),
				"A network owner name is required");
		this.setOwnerName(ownerName);
		this.xgmmlFile = new File(fn);
		this.xgmmlURI = xgmmlFile.toURI().toString();
		this.networkService = new SIFNetworkService();
		this.readDataManager = new ReadDataManager(networkService);
		this.handlerFactory = new HandlerFactory(readDataManager);
		this.parser = new XGMMLParser(this.handlerFactory, this.readDataManager);
		this.xgmmlFileStream = null;
	}  
	
	private void log (String string){
		System.out.println(string);
	}
    
    
	private void setNetwork() throws Exception {
		String title = Files.getNameWithoutExtension(this.xgmmlFile.getName());
		this.networkService.createNewNetwork(this.getOwnerName(), title);
	}

	@Override
	public void parseFile() {
        
        try
        {
            xgmmlFileStream = new FileInputStream(this.getXgmmlFile());
        }
        catch (FileNotFoundException e1)
        {
            log("Could not read " + this.getXgmmlFile());
            this.networkService.rollbackCurrentTransaction();  // close connection to database
            // e1.printStackTrace();
            return;
        }

        try
        {
        	
            setNetwork();
            readXGMML();
            


            // close database connection
         	this.networkService.persistNewNetwork();
        }
        catch (Exception e)
        {
            // rollback current transaction and close the database connection
            this.networkService.rollbackCurrentTransaction();
            e.printStackTrace();
        } 
		
	}
	
	/**
	 * Actual method to read XGMML documents.
	 * 
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	protected void readXGMML() throws SAXException, IOException {
		final SAXParserFactory spf = SAXParserFactory.newInstance();

		try {
			// Get our parser
			final SAXParser sp = spf.newSAXParser();
			// Ignore the DTD declaration
			final XMLReader reader = sp.getXMLReader();
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			reader.setFeature("http://xml.org/sax/features/validation", false);
			// Make the SAX1 Parser act as a SAX2 XMLReader
			final ParserAdapter pa = new ParserAdapter(sp.getParser());
			pa.setContentHandler(parser);
			pa.setErrorHandler(parser);
			// Parse the XGMML input
			pa.parse(new InputSource(xgmmlFileStream));
		} catch (OutOfMemoryError oe) {
			// It's not generally a good idea to catch OutOfMemoryErrors, but in
			// this case, where we know the culprit (a file that is too large),
			// we can at least try to degrade gracefully.
			System.gc();
			throw new RuntimeException("Out of memory error caught. The network being loaded is too large for the current memory allocation.  Use the -Xmx flag for the java virtual machine to increase the amount of memory available, e.g. java -Xmx1G cytoscape.jar -p apps ....");
		} catch (ParserConfigurationException e) {
			logger.error("XGMMLParser: " + e.getMessage());
		} catch (SAXParseException e) {
			logger.error("XGMMLParser: fatal parsing error on line " + e.getLineNumber() + " -- '" + e.getMessage()
					+ "'");
			throw e;
		} finally {
			if (xgmmlFileStream != null) {
				try {
					xgmmlFileStream.close();
				} catch (Exception e) {
					logger.warn("Cannot close XGMML input stream", e);
				}
			}
		}
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public SIFNetworkService getNetworkService() {
		return networkService;
	}

	public void setNetworkService(SIFNetworkService networkService) {
		this.networkService = networkService;
	}

	public File getXgmmlFile() {
		return xgmmlFile;
	}

	public String getXgmmlURI() {
		return xgmmlURI;
	}
	
	

}
