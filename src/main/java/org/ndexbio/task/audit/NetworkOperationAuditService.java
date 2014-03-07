package org.ndexbio.task.audit;



import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/*
 * Represents an NDEx service that an application performing a major operation on
 * an NDEx network can use to track that operation.
 * This class encapsulates a NetworkOperationAudit instance to persist operation metrics
 * 
 */
public class NetworkOperationAuditService {
	
	private NdexOperationMetrics audit;
	private static final Log logger = LogFactory
			.getLog(NetworkOperationAuditService.class);
	private final NetworkIdentifier networkId;
	private Set<NetworkProvenanceRecord> provenanceSet;
	private String networkFileName; // import or export file name
	
	public NetworkOperationAuditService(String  networkName, NdexAuditUtils.NetworkOperation oper){
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkName), "A network name is required");
		Preconditions.checkArgument(null != oper, "A valid network operation is required");
	   Optional<NetworkIdentifier> optId =  NdexAuditUtils.generateNetworkIdentifier(networkName);
	   
	   if( optId.isPresent() ){
		   this.networkId = optId.get();
		   this.audit = new NdexOperationMetrics( oper);
		   this.initializeMetrics();	   
	   }  else {
		   this.networkId = null;
		   logger.fatal("Unable to create NetworkIdentifier for " +networkName);
	   }
	   
	}
	
	private void initializeMetrics(){
		for(String metric : NdexAuditUtils.getNetworkMetricsList){
			this.audit.incrementMeasurement(metric);
		}
	}
	public Set<NetworkProvenanceRecord> getProvenanceSet() {
		return provenanceSet;
	}

	public void addProvenanceRecord(NetworkProvenanceRecord record){
		Preconditions.checkArgument(null != record, 
				"A NetworkProvenanceRecord is required");
		this.provenanceSet.add(record);
	}
	
	public String getNetworkFileName() {
		return networkFileName;
	}

	/* 
	 * we should only be calling this method once, so synchronization
	 * isn't going to cause a big impact
	 */
	public synchronized void setNetworkFileName(String networkFileName) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkFileName), 
				"A network file name is required");
		this.networkFileName = networkFileName;
	}

	public NetworkIdentifier getNetworkId() {
		return networkId;
	}
	

}
