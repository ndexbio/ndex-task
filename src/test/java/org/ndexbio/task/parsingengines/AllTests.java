package org.ndexbio.task.parsingengines;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.task.Configuration;

@RunWith(Suite.class)
@SuiteClasses({ BioPAXParserTest.class, SifParserTest.class,
		xbelParserTest.class, XgmmlParserTest.class })
public class AllTests {

	
	static Configuration configuration ;
	static String propertyFilePath = "/opt/ndex/conf/ndex.properties";
	public static Configuration confituration;
	public static NdexDatabase db ;

	  @BeforeClass 
	    public static void setUpClass() throws NdexException {      
			setEnv();

	    	// read configuration
	    	configuration = Configuration.getInstance();
	    	
	    	//and initialize the db connections
	    	NdexAOrientDBConnectionPool.createOrientDBConnectionPool(
	    			configuration.getDBURL(),
	    			configuration.getDBUser(),
	    			configuration.getDBPasswd(),1);
	    	
	    	db = new NdexDatabase(configuration.getHostURI());

	    }

	  @AfterClass public static void tearDownClass() { 
			db.close();
			NdexAOrientDBConnectionPool.close();
      }

	  
		private static void setEnv()
		{
		  try
		    {
		        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
		        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
		        theEnvironmentField.setAccessible(true);
		        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
		        env.put("ndexConfigurationPath", propertyFilePath);
		        //env.putAll(newenv);
		        Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
		        theCaseInsensitiveEnvironmentField.setAccessible(true);
		        Map<String, String> cienv = (Map<String, String>)     theCaseInsensitiveEnvironmentField.get(null);
		        //cienv.putAll(newenv);
		        env.put("ndexConfigurationPath", propertyFilePath);
		    }
		    catch (NoSuchFieldException e)
		    {
		      try {
		        Class[] classes = Collections.class.getDeclaredClasses();
		        Map<String, String> env = System.getenv();
		        for(Class cl : classes) {
		            if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
		                Field field = cl.getDeclaredField("m");
		                field.setAccessible(true);
		                Object obj = field.get(env);
		                Map<String, String> map = (Map<String, String>) obj;
		                //map.clear();
		                //map.putAll(newenv);
		                map.put("ndexConfigurationPath", propertyFilePath);
		            }
		        }
		      } catch (Exception e2) {
		        e2.printStackTrace();
		      }
		    } catch (Exception e1) {
		        e1.printStackTrace();
		    } 
		}

}
