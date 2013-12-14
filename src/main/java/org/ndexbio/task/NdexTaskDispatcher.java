package org.ndexbio.task;

import java.util.List;
import java.util.TimerTask;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.orientdb.persistence.NdexTaskService;
import org.ndexbio.rest.models.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * represents a class that is invoked at a specified interval by a cron process. When invoked,
 * this class will check for tasks in the orientdb database with a status of pending
 * Based on the type of task queued, this class will invoke the appropriate type
 * of NdexTaskRunner. This later class will be responsible for completing the queued
 * task
 */

public class NdexTaskDispatcher  {
	
	private static final Logger logger = LoggerFactory.getLogger(NdexTaskDispatcher.class);
	private static final Integer MAX_TASKS = 1;
	private final NdexTaskService taskService = new NdexTaskService();

	public NdexTaskDispatcher(){
		
	}
	
	public static void main(String[] args) {
		logger.info(" invoked");
		NdexTaskDispatcher dispatcher = new NdexTaskDispatcher();
		dispatcher.dispatchTasks();

	}
	/*
	 * dispatch pending tasks up to the maximum limit
	 */
	private void dispatchTasks() {
		List<Task> taskList = taskService.getPendingFileUploads();
		logger.info(taskList.size() +" file upload tasks are pending");
		int taskLimit = Math.min(taskList.size(), MAX_TASKS);
		int dispatchCount = 0;
		while (dispatchCount < taskLimit){
			try {
				String taskId = taskList.get(dispatchCount).getId();
				NdexTask newTask = NdexTaskFactory.INSTANCE.getNdexTaskByTaskType(taskId);
				new Thread(newTask).start();
				logger.info("Task id: " + taskId +" started");
			} catch (IllegalArgumentException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			dispatchCount++;
			
		}
		
	}
	

}
