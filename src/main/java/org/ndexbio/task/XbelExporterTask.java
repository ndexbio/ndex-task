package org.ndexbio.task;

import java.io.File;
import java.io.IOException;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.ITask;
import org.ndexbio.common.models.object.Status;
import org.ndexbio.task.event.NdexTaskEventHandler;
import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.task.service.NdexTaskModelService;
import org.ndexbio.xbel.exporter.XbelNetworkExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Represents an NdexTask subclass responsible for exporting an XBEL network
 * from the NDEx database to an external file in XML format that adheres to the
 * XBEL schema. If the task type is KAMCOMPILE, the class is also responsible
 * for creating a new Task enrty in the database indicating that the new XML
 * file should be processed by the Kam compiler.
 * 
 */

public class XbelExporterTask extends NdexTask {
	
	private String networkId;
	private static final String NETWORK_EXPORT_PATH = "/opt/ndex/exported-networks/";
	private static final String NETWORK_EXPORT_EVENT_PATH = "/opt/ndex/exported-networks-events/";
	private static final String XBEL_FILE_EXTENSION = ".xbel";
	private static final String EVENT_FILE_EXTENSION = ".csv";
	private final NdexTaskModelService modelService;
	private NdexTaskEventHandler eventHandler;
	private Status taskStatus;
	
	
	private static final Logger logger = LoggerFactory
			.getLogger(XbelExporterTask.class);
	
	public XbelExporterTask(String taskId) throws
		IllegalArgumentException, SecurityException, NdexException{
		
			super(taskId);
			this.networkId = this.getTask().getResource();
			this.modelService = new NdexJVMDataModelService();
			
	}

	@Override
	public ITask call() throws Exception {
		try {
			String eventFilename = 
					this.resolveFilename(this.NETWORK_EXPORT_EVENT_PATH, this.EVENT_FILE_EXTENSION);
			this.eventHandler = new NdexTaskEventHandler(eventFilename);
			this.exportNetwork();
			return this.getTask();
		} catch (InterruptedException e) {
			logger.info(this.getClass().getName() +" interupted");
			return null;
		} finally {
			if (null != this.eventHandler) {
				this.eventHandler.shutdown();
			}
		}
	}
	
	/*
	 * private method to invoke the xbel network exporter
	 */
	private void exportNetwork() throws Exception{
		this.taskStatus = Status.PROCESSING;
		this.startTask();
		String exportFilename = this.resolveFilename(this.NETWORK_EXPORT_PATH, this.XBEL_FILE_EXTENSION);
		
		XbelNetworkExporter exporter = new XbelNetworkExporter(this.getUserId(),
				this.networkId,
				 this.modelService, exportFilename);
		exporter.exportNetwork();
		this.taskStatus = Status.COMPLETED;
		this.updateTaskStatus(this.taskStatus);
	}
	
	/*
	 * private method to resolve the filename for the exported file
	 * Current convention is to use a fixed based directory under /opt/ndex
	 * add a subdriectory based on the username and use the network name plus the
	 * xbel extension as a filename
	 */
	private String resolveFilename(String path, String extension) {
		StringBuilder sb = new StringBuilder(path);
		sb.append(File.separator);
		sb.append(this.getTask().getOwner().getUsername());
		if (! new File(sb.toString()).exists()) {
			new File(sb.toString()).mkdir();
		}
		sb.append(File.separator);
		sb.append(this.modelService.getNetworkById(this.getUserId(),networkId).getName());
		sb.append(extension);
		return sb.toString();		
	}
	
	

	protected String getNetworkId() {
		return networkId;
	}

	private void setNetworkId(String networkId) {
		this.networkId = networkId;
	}

}
