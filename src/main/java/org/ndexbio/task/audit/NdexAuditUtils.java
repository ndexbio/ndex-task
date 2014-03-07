package org.ndexbio.task.audit;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.google.common.base.Optional;


/*
 * Represents a collection of static utility methods 
 * to support network operations auditing
 */
public class NdexAuditUtils {
	

	public static enum NetworkOperation {
		IMPORT, EXPORT, COPY, SUBSET
	};
	
	private static final String[] networkMetrics = {"edge count", "function term count",
		"base term count", "citation count"
	};
	
	public static List<String> getNetworkMetricsList = Arrays.asList(networkMetrics);
	
	public static UUID generateNetworkUUID(String networkName, URI networkURI) {
		return UUID.fromString(networkName +networkURI);
	}
	
	public static NetworkIdentifier generateNetworkIdentifier( String networkName,
			URI networkURI){
		UUID uuid = generateNetworkUUID(networkName, networkURI);
		return new NetworkIdentifier(uuid, networkName, networkURI);
	}
	// use URI for localhost
	public static Optional<NetworkIdentifier> generateNetworkIdentifier( String networkName){
		
		 NetworkIdentifier networkId = null;
		try {
			URI networkURI  = new URI("http://localhost:8080/ndexbio/");
			UUID uuid = generateNetworkUUID(networkName, networkURI);
			networkId=  new NetworkIdentifier(uuid, networkName, networkURI);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Optional.of(networkId);
	}

}
