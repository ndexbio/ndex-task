package org.ndexbio.task.service;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.easymock.EasyMock;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.object.Citation;
import org.ndexbio.common.models.object.Edge;
import org.ndexbio.common.models.object.Namespace;
import org.ndexbio.common.models.object.NdexDataModelService;
import org.ndexbio.common.models.object.Network;
import org.ndexbio.rest.services.NetworkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;



public class NdexJVMDataModelService implements NdexDataModelService {
	
	/*
	 * we need a mock HttpServletRequest object to use the RESTul service operations
	 */
	 final HttpServletRequest mockRequest = EasyMock.createMock(HttpServletRequest.class);
	 final NetworkService networkService = new NetworkService(mockRequest);
	 private static final Logger logger = LoggerFactory
				.getLogger(NdexJVMDataModelService.class);
	 private final int SKIP = 0;
	 private final int TOP = Integer.MAX_VALUE;

	@Override
	public Network getNetworkById(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return networkService.getNetwork(networkId);
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	
	@Override
	public Iterable<Citation> getCitationsByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		try {
			return networkService.getCitations(networkId, SKIP, TOP);
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return new ArrayList<Citation>();
	}

	@Override
	public Iterable<Edge> getEdgesBySupportId(String supportId) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Iterable<Namespace> getNamespacesByNetworkId(String networkId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId),
				"A network id is required");
		try {
			return networkService.getNamespaces(networkId, 0, 1000);
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		return new ArrayList<Namespace>();
	}


	@Override
	public Network getSubnetworkByCitationId(String networkId,
			String citationId) {
		Preconditions.checkArgument(!Strings.isNullOrEmpty(networkId), 
				"A network id is required");
		Preconditions.checkArgument(!Strings.isNullOrEmpty(citationId), 
				"A citation id is required");
		try {
			return networkService.getEdgesByCitations(networkId, SKIP, TOP, new String[] {citationId});
		} catch (IllegalArgumentException | NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		return null;
	}

}
