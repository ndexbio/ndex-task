package org.ndexbio.task;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.data.TaskType;
import org.ndexbio.common.models.object.Task;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;

/*
 * Singleton responsible for instantiating the appropriate implementation
 * of NdexTask based on the Task type
 */
 enum NdexTaskFactory {
	INSTANCE;
	
	NdexTask getNdexTaskByTaskType(String taskId){
		NdexTaskService taskService = new NdexTaskService();
		try {
			Task task = taskService.getITask(taskId);
			if( task.getType() == TaskType.PROCESS_UPLOADED_NETWORK) {
				return new FileUploadTask(taskId);
			}
			throw new IllegalArgumentException("Task type: " +task.getType() +" is not supported");
		} catch (IllegalArgumentException | SecurityException | NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}
}
