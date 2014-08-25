package org.ndexbio.task;

import java.io.File;
import java.io.IOException;

import org.ndexbio.task.event.NdexNetworkState;
import org.ndexbio.task.event.NdexTaskEventHandler;
import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.task.service.NdexTaskModelService;
import org.ndexbio.xbel.exporter.XbelNetworkExporter;
import org.ndexbio.xbel.exporter.XbelNetworkExporter.XbelMarshaller;

public class TestXbelExporterApp {

	private static final String NETWORK_EXPORT_PATH = "C:/tmp/ndex/exported-networks/";
	private static final String XBEL_FILE_EXTENSION = ".xbel";
	public static void main(String[] args) throws IOException {
		//String networkId = "C25R732"; // is for large corpus
		String networkId = "3eda7195-2bac-11e4-9f09-001f3bca188f"; // is for small corpus
		String userId = "7ce190f0-27ec-11e4-a273-90b11c72aefa"; // dbowner
		//add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("TextXbelExporter completed.");
			}
		});
		
		NdexTaskModelService  modelService = new NdexJVMDataModelService();
		// initiate the network state
		initiateStateForMonitoring(modelService, userId, networkId);
		NdexTaskEventHandler eventHandler = new NdexTaskEventHandler("C:/tmp/ndextaskevents.csv");
		XbelNetworkExporter exporter = new XbelNetworkExporter(userId, networkId, 
				modelService,
				resolveExportFile(modelService, userId, networkId));
		//
		exporter.exportNetwork();
		eventHandler.shutdown();

	}

	private static String resolveExportFile(NdexTaskModelService  modelService, 
			String userId, String networkId) {
		StringBuilder sb = new StringBuilder(NETWORK_EXPORT_PATH);
		
		sb.append(userId);
//		if (! new File(sb.toString()).exists()) {
			new File(sb.toString()).mkdirs();
//		}
		sb.append(File.separator);
		sb.append(modelService.getNetworkById(networkId).getName());
		sb.append(XBEL_FILE_EXTENSION);
		System.out.println("Export file: " +sb.toString());
		return sb.toString();
	
	}
	
	private static void initiateStateForMonitoring(NdexTaskModelService  modelService, 
			String userId,
			String networkId) {
		NdexNetworkState.INSTANCE.setNetworkId(networkId);
		NdexNetworkState.INSTANCE.setNetworkName(modelService.getNetworkById( networkId).getName());
		
		
	}
	
}
