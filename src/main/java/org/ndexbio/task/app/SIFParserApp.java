package org.ndexbio.task.app;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.orientdb.domain.INetwork;
import org.ndexbio.orientdb.domain.INetworkMembership;
import org.ndexbio.orientdb.domain.IUser;
import org.ndexbio.orientdb.domain.Permissions;
import org.ndexbio.rest.models.SearchParameters;
import org.ndexbio.rest.models.SearchResult;
import org.ndexbio.task.sif.SIFFileParser;
import org.ndexbio.task.sif.service.SIFNetworkService;
import org.ndexbio.xbel.service.XBelNetworkService;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

/*
 * Java application to evaluate parsing a specified file in XBEL format
 */

public class SIFParserApp {

	private static String testUserName = "dexterpratt";

	public static void main(String[] args) throws Exception {
		String filename = null;
		if (args.length > 0) {
			filename = args[0];
		} else {
			//filename = "gal-filtered.sif";
			filename = "Glucocorticoid_receptor_regulatory_network.SIF";
		}
		try {
			SIFFileParser parser = new SIFFileParser(filename);
			parser.setNetwork(SIFNetworkService.getInstance().createNewNetwork());
			IUser networkOwner = resolveUserUserByUsername(testUserName);
			INetworkMembership membership = XBelNetworkService.getInstance()
					.createNewMember();
			membership.setMember(networkOwner);
			membership.setPermissions(Permissions.ADMIN);
			INetwork network = parser.getNetwork();
			membership.setNetwork(network);
			network.setIsPublic(true);  // for convenience of debugging, start this as public
			networkOwner.addNetwork(membership);
            network.addMember(membership);
			
			parser.getNetwork().setTitle(parser.getSifFile().getName());
			parser.parseSIFFile();

			for (String msg : parser.getMsgBuffer()) {
				System.out.println(msg);
			}

		} catch (Exception e) {
			// Clean up: delete the network if exception caught
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}

	private static IUser resolveUserUserByUsername(String userName) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(userName),
				"A username is required");
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setSearchString(userName);
		searchParameters.setSkip(0);
		searchParameters.setTop(1);

		try {
			SearchResult<IUser> result = XBelNetworkService.getInstance()
					.findUsers(searchParameters);
			return (IUser) result.getResults().iterator().next();

		} catch (NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
