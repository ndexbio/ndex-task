package org.ndexbio.task;

import org.ndexbio.common.models.data.ITask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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
	
	
	private static final Logger logger = LoggerFactory
			.getLogger(XbelExporterTask.class);

	public XbelExporterTask(ITask itask) {
		super(itask);
		Preconditions.checkArgument(null != itask, "An ITask object is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(itask.getResource()), 
				 "The ITask entry must specify a network id");
		
		this.networkId =itask.getResource();
	}

	@Override
	public ITask call() throws Exception {
		try {
			this.exportNetwork();
			return this.getTask();
		} catch (InterruptedException e) {
			logger.info(this.getClass().getName() +" interupted");
			return null;
		}
	}
	
	private void exportNetwork() throws Exception{
		
	}
	
	

	protected String getNetworkId() {
		return networkId;
	}

	private void setNetworkId(String networkId) {
		this.networkId = networkId;
	}

}
