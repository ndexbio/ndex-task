package org.ndexbio.xbel.service;

import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.xbel.service.XBelNetworkService;

/*
 * A Singleton to provide a instance of a test network with fixed metadata
 * 
 * FOR TESTING PURPOSES ONLY
 */
public enum OrientdbNetworkFactory {
	INSTANCE;
	private String testUserName = "jstegall";
	
	public INetwork createTestNetwork(String title) throws Exception {
		 final INetwork testNetwork = XBelNetworkService.getInstance().createNewNetwork(testUserName, title);
			XBelNetworkService.getInstance().commitCurrentNetwork();
		 return  testNetwork;
	}
	
}
