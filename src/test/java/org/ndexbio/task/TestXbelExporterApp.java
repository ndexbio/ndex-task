package org.ndexbio.task;

import java.io.File;
import java.io.IOException;

import org.ndexbio.common.access.NdexAOrientDBConnectionPool;
import org.ndexbio.task.event.NdexNetworkState;
import org.ndexbio.task.event.NdexTaskEventHandler;
import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.task.service.NdexTaskModelService;
import org.ndexbio.xbel.exporter.XbelNetworkExporter;
import org.ndexbio.xbel.exporter.XbelNetworkExporter.XbelMarshaller;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class TestXbelExporterApp {

	private static final String NETWORK_EXPORT_PATH = "c:/tmp/ndex/exported-networks/";
	private static final String XBEL_FILE_EXTENSION = ".xbel";
	public static void main(String[] args) throws IOException {

		String networkId = "0ef6614e-3bb3-11e4-a7e4-001f3bca188f"; // is for small corpus
		String userId =    "c9ed4e57-3bb1-11e4-8208-001f3bca188f"; // dbowner
		//add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("TextXbelExporter completed.");
			}
		});
		
		ODatabaseDocumentTx db = null;
		try {
			
			db = NdexAOrientDBConnectionPool.getInstance().acquire();
			NdexTaskModelService  modelService = new NdexJVMDataModelService(db);
			// initiate the network state
			initiateStateForMonitoring(modelService, userId, networkId);
			NdexTaskEventHandler eventHandler = new NdexTaskEventHandler("c:/tmp/ndextaskevents.csv");
			XbelNetworkExporter exporter = new XbelNetworkExporter(userId, networkId, 
				modelService,
				resolveExportFile(modelService, userId, networkId));
		//
			exporter.exportNetwork();
			eventHandler.shutdown();
		} finally { 
			if ( db != null) db.close();
		}
		
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
