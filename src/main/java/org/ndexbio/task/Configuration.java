package org.ndexbio.task;

import java.io.FileReader;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.ndexbio.common.exceptions.NdexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration
{
    public static final String UPLOADED_NETWORKS_PATH_PROPERTY = "Uploaded-Networks-Path";
    
    private static Configuration INSTANCE = null;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private Properties _configurationProperties;
    
	private static String dbURL;
	private static final String dbUserPropName 	   = "OrientDB-Username";
	private static final String dbPasswordPropName = "OrientDB-Password";
	    
	private static String hostURI ;
   
    
    /**************************************************************************
    * Default constructor. Made private to prevent instantiation. 
     * @throws NdexException 
    **************************************************************************/
    private Configuration() throws NdexException
    {
        try
        {
        	String configFilePath = System.getenv("ndexConfigurationPath");
        	
        	if ( configFilePath == null) {
        		InitialContext ic = new InitialContext();
        	    configFilePath = (String) ic.lookup("java:comp/env/ndexConfigurationPath"); 
        	}    
        	
        	if ( configFilePath == null) {
        		_logger.error("ndexConfigurationPath is not defined in environement.");
        		throw new NdexException("ndexConfigurationPath is not defined in environement.");
        	} 
        	
        	_logger.info("Loadinging ndex configuration from " + configFilePath);
        	
        	_configurationProperties = new Properties();
        	FileReader reader = new FileReader(configFilePath);
        	_configurationProperties.load(reader);
        	reader.close();
            
            dbURL = _configurationProperties.getProperty("OrientDB-URL");
            
            if ( dbURL == null) {
            	throw new NdexException ("property " + "OrientDB-URL" + " not found in configuration.");
            }
            
            hostURI = _configurationProperties.getProperty("HostURI");
            if ( hostURI == null) {
            	throw new NdexException ("property " +  "HostURI" + " not found in configuration.");
            }

        }
        catch (Exception e)
        {
            _logger.error("Failed to load the configuration file.", e);
            throw new NdexException ("Failed to load the configuration file. " + e.getMessage());
        }
    }
    
    
    
    /**************************************************************************
    * Gets the singleton instance. 
     * @throws NdexException 
    **************************************************************************/
    public static Configuration getInstance() throws NdexException
    {
    	if ( INSTANCE == null)  { 
    		INSTANCE = new Configuration();
    	}
        return INSTANCE;
    }

    
    
    /**************************************************************************
    * Gets the singleton instance. 
    * 
    * @param propertyName
    *            The property name.
    **************************************************************************/
    public String getProperty(String propertyName)
    {
        return _configurationProperties.getProperty(propertyName);
    }
    
    /**************************************************************************
    * Gets the singleton instance. 
    * 
    * @param propertyName
    *            The property name.
    * @param propertyValue
    *            The property value.
    **************************************************************************/
    public void setProperty(String propertyName, String propertyValue)
    {
        _configurationProperties.setProperty(propertyName, propertyValue);
    }
    
    public String getDBURL () { return dbURL; }
    public String getDBUser() { return _configurationProperties.getProperty(dbUserPropName); }
    public String getDBPasswd () { return _configurationProperties.getProperty(dbPasswordPropName); }
    public String getHostURI () { return hostURI; } 
}
