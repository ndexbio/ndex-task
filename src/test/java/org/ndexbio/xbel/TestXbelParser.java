package org.ndexbio.xbel;


import org.ndexbio.service.ResourceMonitor;
import org.ndexbio.xbel.parser.XbelFileValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ndexbio.xbel.parser.XbelFileValidator.ValidationState;

/* java application to test parsing an XBEL document using
 * partial JAXB processing. This test does not persist any data to
 * a database. It monitors memory usage
 * 
 */
public class TestXbelParser {
	private static final Logger logger = LoggerFactory.getLogger(TestXbelParser.class);
	private final String xbelFileName;
	private final ValidationState validationState;
	
	public TestXbelParser(String fn) {
		this.xbelFileName = fn;
		logger.info("Validating XBEL file: " +fn);
		logger.info("Memory usage prior to validation = " +
		ResourceMonitor.INSTANCE.getMemoryMbUsage().toString() +" Mb");
		this.validationState = new XbelFileValidator(fn).getValidationState();
		logger.info("Validation state = " +this.validationState.getValidationMessage());
		logger.info("Memory usage post validation = " +
				ResourceMonitor.INSTANCE.getMemoryMbUsage().toString() +" Mb");
		if(this.validationState.isValid()){
			this.parseXBelFile();
		}
	}
	
	private void parseXBelFile() {
		
	}
	
	public static void main(String[] args) throws Exception {
		String filename = null;
		if(args.length > 0 ){
			filename = args[0];
		} else {
			 filename = "small_corpus.xbel";
		}
		TestXbelParser parser = new TestXbelParser(filename);
		
	}
}
