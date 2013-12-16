package org.ndexbio.task;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
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
	private static final Integer MAX_THREADS = 1;
	private final NdexTaskService taskService = new NdexTaskService();
	private final  CompletionService<Task> taskCompletionService;
	

	public NdexTaskDispatcher(){
		 ExecutorService taskExecutor = Executors.newFixedThreadPool(MAX_THREADS);
	       this.taskCompletionService =
	           new ExecutorCompletionService<Task>(taskExecutor);
	}
	
	public static void main(String[] args) {
		logger.info(" invoked");
		NdexTaskDispatcher dispatcher = new NdexTaskDispatcher();
		dispatcher.processTasks();
		

	}
	
	private void processTasks() {
		try {
			Integer activeTasks = this.determineInProgressTasks();
			if(activeTasks < MAX_THREADS) {
				this.dispatchTasks((MAX_THREADS-activeTasks));
			} else {
				logger.info("The number of in progress tasks is already at the maximum value");
				logger.info("The check for newly queued tasks was skipped");
			}
		} catch (NdexException e) {
			logger.error("Error processing tasks");
		}
	}
	
	/*
	 * terminate in processing tasks which have exceeded the maximum time limit
	 */
	private  Integer determineInProgressTasks() throws NdexException {
		List<Task> taskList = taskService.getInProgressTasks();
		Integer activeTasks = taskList.size();
		// for now just print out the in progress tasks
		for ( Task task : taskList){
			//TODO check running time and terminate job if necessary
			// decrement activeTasks 
			logger.info("Task id " +task.getId() +" is in progress");
		}
		return activeTasks;
	}
	
	
	/*
	 * dispatch pending tasks up to the maximum limit
	 */
	private void dispatchTasks(Integer numTasks) throws NdexException {
		
		List<Task> taskList = taskService.getQueuedTasks();
		logger.info(taskList.size() +" file upload tasks are pending");
		int taskLimit = Math.min(taskList.size(), numTasks);
		int submittedTasks = 0;
		while (submittedTasks < taskLimit){
			try {
				String taskId = taskList.get(submittedTasks).getId();
				NdexTask newTask = NdexTaskFactory.INSTANCE.getNdexTaskByTaskType(taskId);
				this.taskCompletionService.submit(newTask);
				logger.info("Task id: " + taskId +" started");
				
			} catch (IllegalArgumentException | SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			submittedTasks++;		
		}
		
		// monitor submitted jobs until completion
		for(int tasksCompleted=0;tasksCompleted<submittedTasks;tasksCompleted++){
	        try {
	            logger.debug("trying to take from Completion service");
	            Future<Task> result = taskCompletionService.take();
	            System.out.println("result for a task availble in queue.Trying to get()"  );
	            // the result contains a Task with an updated status
	            this.postTaskCompleteion(result.get());
	        } catch (InterruptedException e) {
	            
	            logger.error("Error Interrupted exception");
	            e.printStackTrace();
	        } catch (ExecutionException e) {
	            // Something went wrong with the result
	            e.printStackTrace();
	            logger.error("Error get() threw exception");
	        } 
	        
	    }
		
	}
	
	private void postTaskCompleteion(Task completedTask) throws 
		IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException {
		
			taskService.updateTask(completedTask);
			logger.info("Completion status for task id: " +completedTask.getId() +
					"is " +completedTask.getStatus().toString());
		
		
	}

	
	

}
