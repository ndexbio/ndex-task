package org.ndexbio.task;

import java.io.File;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.helpers.Configuration;
import org.ndexbio.common.models.data.Status;
import org.ndexbio.common.models.object.Task;
import org.ndexbio.task.parsingengines.*;
import org.ndexbio.xbel.parser.XbelFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class represents a NdexTask subclass that is responsible
 * for uploading a specified data file into a new NDEx network in
 * orientdb. A particular file parser is selected based on the file type.
 * Since this class is invoked based on a Task registered in the orientdb 
 * database, no user authentication is required.
 * 
 */

public class FileUploadTask extends NdexTask {

	private final String filename;
	private static final Logger logger = LoggerFactory
			.getLogger(FileUploadTask.class);

	private Status taskStatus;

	public FileUploadTask(String taskId) throws IllegalArgumentException,
			SecurityException, NdexException {
		super(taskId);
		 this.filename = Configuration.getInstance().getProperty(
		Configuration.UPLOADED_NETWORKS_PATH_PROPERTY)
		 + this.getTask().getResource();
		//this.filename = this.getTask().getResource();
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

	private void processFile() throws Exception {
		logger.info("Processing file: " + this.getFilename());
		this.taskStatus = Status.PROCESSING;
		this.startTask();
		File file = new File(this.getFilename());
		switch (com.google.common.io.Files.getFileExtension(this.getFilename())) {
		case ("sif"):
			try {
				final SifParser sifParser = new SifParser(
						file.getAbsolutePath(),this.getTask().getOwner().getUsername());
				sifParser.parseFile();
				this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus = Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
			break;
		case ("xbel"):
			try {
				final XbelFileParser xbelParser = new XbelFileParser(
						file.getAbsolutePath(),this.getTask().getOwner().getUsername());

				if (!xbelParser.getValidationState().isValid()) {
					this.taskStatus = Status.COMPLETED_WITH_ERRORS;
					throw new NdexException(
							"XBEL file fails XML schema validation - one or more elements do not meet XBEL specification.");
				}
				xbelParser.parseXbelFile();
				this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus = Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
			break;
		case ("xlsx"):
		case ("xls"):
			try {
				final ExcelParser excelParser = new ExcelParser(
						file.getAbsolutePath(),this.getTask().getOwner().getUsername());
				excelParser.parseFile();
				this.taskStatus = Status.COMPLETED;
			} catch (Exception e) {
				this.taskStatus = Status.COMPLETED_WITH_ERRORS;
				logger.error(e.getMessage());
			}
		default:
			file.delete();
			logger.error("The uploaded file type is not supported; must be SIF, XBEL, XLS or XLSX.");
			this.taskStatus = Status.COMPLETED_WITH_ERRORS;

		}
		this.updateTaskStatus(this.taskStatus);
	}

}
