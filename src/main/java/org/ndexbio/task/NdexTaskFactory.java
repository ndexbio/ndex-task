package org.ndexbio.task;

import java.util.logging.Logger;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.TaskType;
import org.ndexbio.model.object.network.FileFormat;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;

/*
 * Singleton responsible for instantiating the appropriate implementation
 * of NdexTask based on the Task type
 */
 enum NdexTaskFactory {
	INSTANCE;
	
	private NdexTaskFactory(){
	}
	
	NdexTask getNdexTaskByTaskType(Task task){
		
		try {
			if( task.getTaskType() == TaskType.PROCESS_UPLOADED_NETWORK) {
				return new FileUploadTask(task);
			}
			if( task.getTaskType() == TaskType.EXPORT_NETWORK_TO_FILE) {
				if ( task.getFormat() == FileFormat.XBEL)
					return new XbelExporterTask(task);
				
				throw new NdexException ("Only XBEL exporter is implemented.");
			}
			throw new IllegalArgumentException("Task type: " +task.getType() +" is not supported");
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			
		}
		return null;
	}
	
}
