package org.ndexbio.xbel.service;

import java.util.ArrayList;
import java.util.List;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.INetwork;
import org.ndexbio.common.models.data.INetworkMembership;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.data.Permissions;
import org.ndexbio.common.models.object.*;
import org.ndexbio.xbel.service.XBelNetworkService;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

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
