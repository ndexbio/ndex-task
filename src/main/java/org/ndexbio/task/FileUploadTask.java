package org.ndexbio.task;

import java.io.File;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.orientdb.domain.Status;
import org.ndexbio.rest.models.Task;
import org.ndexbio.service.helpers.Configuration;
import org.ndexbio.task.exel.parser.ExcelFileParser;
import org.ndexbio.task.sif.SIFFileParser;
import org.ndexbio.xbel.parser.XbelFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class represents a NdexTask subclass that is responsible
 * for uploading a specified data file into a new NDEx network in
 * orientdb. A paricular file parser is slected based on the file type.
 * Since this class is invoked based on a Task registered in the orientdb 
 * database, no user authentication is required.
 * 
 */

public class FileUploadTask extends NdexTask {

	private final String filename;
	private static final Logger logger = LoggerFactory.getLogger(FileUploadTask.class);
	
	private Status taskStatus;

	public FileUploadTask(String taskId) throws IllegalArgumentException,
			SecurityException, NdexException {
		super(taskId);
		this.filename = Configuration.getInstance().getProperty(
				Configuration.UPLOADED_NETWORKS_PATH_PROPERTY)
				+ this.getTask().getResource();
		if (!(new File(this.filename).isFile())) {
			throw new NdexException("File " + this.filename + " does not exist");
		}
	}

	

	@Override
	public Task call() throws Exception {
		this.processFile();
		return this.getTask();
	}

	
	protected String getFilename() {
		return this.filename;
		
	}
	
	private void processFile ()  {
		logger.info("Processing file: " +this.getFilename());
		this.taskStatus = Status.PROCESSING;
		File file = new File(this.getFilename());
		switch(com.google.common.io.Files.getFileExtension(this.getFilename()) ){
	      case("sif"):
	    	  try {
				final SIFFileParser sifParser = new SIFFileParser(file.getAbsolutePath());
				  sifParser.parseSIFFile();
				  this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus =Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
	      break;
	      case("xbel"):
	    	  try {
				final XbelFileParser xbelParser = new XbelFileParser(file.getAbsolutePath());
					if (!xbelParser.getValidationState().isValid()){
						this.taskStatus =Status.COMPLETED_WITH_ERRORS;
						throw new NdexException("XBEL file is has invalid elements.");
					}	
					xbelParser.parseXbelFile();
					 this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus =Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
	      break;
	      case("xlsx"):
	      case("xls"):
	    	  try {
				final ExcelFileParser excelParser = new ExcelFileParser(file.getAbsolutePath());
				  excelParser.parseExcelFile();
				  this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus =Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
	      default:
	         file.delete();
	        logger.error
	         	("The uploaded file type is not supported; must be SIF, XBEL, or XLSX.");
	        this.taskStatus =Status.COMPLETED_WITH_ERRORS;
	         
	}
	}

	

}
