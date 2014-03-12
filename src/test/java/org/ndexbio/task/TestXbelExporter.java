package org.ndexbio.task;

import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.xbel.exporter.XbelNetworkExporter;

public class TestXbelExporter {

	public static void main(String[] args) {
		String networkId = "C25R14"; // is for small corpus
		//add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("TextXbelExporter completed.");
			}
		});
		XbelNetworkExporter exporter = new XbelNetworkExporter(networkId, 
				new NdexJVMDataModelService() );
		exporter.exportNetwork();

	}

}
