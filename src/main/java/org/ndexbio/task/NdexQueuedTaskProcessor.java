package org.ndexbio.task;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.ITask;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javassist.tools.rmi.ObjectNotFoundException;

import com.google.common.collect.Queues;
import com.orientechnologies.orient.core.id.ORID;
import com.tinkerpop.frames.VertexFrame;

public class NdexQueuedTaskProcessor {
	
	/*
	 * Represents a java application responsible for processing Ndex tasks
	 * persisted in the database with a status of QUEUED. The application
	 * is intended to be invoked on a scheduled basis as a UNIX/Linx cron task.
	 * Each invocation of this application will select all queued tasks from the
	 * orientdb database. The status of these tasks will be updated to STAGED and 
	 * the task ids placed in FIFO queue. The application will process all the STAGED
	 * entries, the level of concurrency will be set by a parameter value. As each task
	 * is removed from the queue, an appropriate task handler will be invoked.
	 * The individual task handlers are responsible for update the task status in the
	 * orientdb database from STAGED to PROCESSING and then to a final completion status.
	 * 
	 * Because a number of tasks may be processed by a single cron invocation of this
	 * application. The application is responsible for checking if any tasks in the database
	 * have a status of STAGED or PROVESSING. If so, that invocation will log the tasks found
	 * with those statuses and terminate. The intent is to have only one invocation running at a time.
	 * 
	 * New tasks queued to the database while this application is running will not
	 * be processed until the next cron invocation of the application.
	 * 
	 */
	private static final Logger logger = LoggerFactory.getLogger(NdexQueuedTaskProcessor.class);
	private static final Integer MAX_THREADS = 1;
	private final NdexTaskService taskService = new NdexTaskService();
	private final  CompletionService<ITask> taskCompletionService;
	private final ConcurrentLinkedQueue<ITask> taskQueue;
	
	
	public NdexQueuedTaskProcessor() {
		ExecutorService taskExecutor = Executors.newFixedThreadPool(MAX_THREADS);
	       this.taskCompletionService =
	           new ExecutorCompletionService<ITask>(taskExecutor);
	       this.taskQueue = Queues.newConcurrentLinkedQueue();
	}
	
	/*
	 * provate method to initiate processing of tasks persisted
	 * with a QUEUED status. Processing proceeds only if there are
	 * no tasks with a status of STAGED or PROCESSING (i.e. active)
	 */
	private void processTasks() {
		try {
			Integer activeTaskCount = this.determineActiveTasks();
			if (activeTaskCount > 0){
				logger.info("There are currently " + activeTaskCount 
						+" active tasks ");
				logger.info("The check for newly queued tasks was skipped");
			} else {
				this.processQueuedITasks();
			}
		} catch (NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	/*
	 * private method to process all the tasks current persisted with a 
	 * QUEUED status
	 */
	private void processQueuedITasks() {
		try {
			List<ITask> stagedTasks = taskService.stageQueuedITasks();
			this.taskQueue.addAll(stagedTasks);
			logger.info("The task queue contains" +this.taskQueue.size() +" staged tasks");
			while (this.taskQueue.size() >0){
				int taskLimit = Math.min(this.taskQueue.size(), MAX_THREADS);
				int submittedTasks = 0;
				while( submittedTasks < taskLimit){
					String taskId = resolveVertexId(this.taskQueue.poll());
					NdexTask newTask = NdexTaskFactory.INSTANCE.getNdexTaskByTaskType(taskId);
					this.taskCompletionService.submit(newTask);
					logger.info("Task id: " + taskId +" started");
					submittedTasks++;
				}
				// monitor submitted jobs until completion
				for(int tasksCompleted=0;tasksCompleted<submittedTasks;tasksCompleted++){
			        try {
			            logger.debug("trying to take from Completion service");
			            Future<ITask> result = taskCompletionService.take();
			            System.out.println("result for a task availble in queue.Trying to get()"  );
			            // the result contains a ITask with an updated status
			            try {
							this.postTaskCompleteion(result.get());
						} catch (IllegalArgumentException | SecurityException
								| ObjectNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
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
			
		} catch (NdexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/*
	 * method to determine the number of tasks that are currently marked
	 * as STAGED or PROCESSING. If >0, it's an indication that an previous 
	 * invocation of this application is still running
	 */
	private  Integer determineActiveTasks() throws NdexException {
		List<ITask> taskList = taskService.getActiveITasks();
		Integer activeTasks = taskList.size();
		// for now just print out the in progress tasks
		for ( ITask task : taskList){
			//TODO check running time and terminate job if necessary
			// decrement activeTasks 
			logger.info("Task id " +resolveVertexId(task) +" has an active status of " 
					+task.getStatus());
		}
		return activeTasks;
	}
	
	private void postTaskCompleteion(ITask completedTask) throws 
	IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException {
	
		taskService.updateTask(completedTask);
		logger.info("Completion status for task id: " +resolveVertexId(completedTask) +
				"is " +completedTask.getStatus().toString());
	
}

	public static void main(String[] args) {
		logger.info("invoked");
		NdexQueuedTaskProcessor taskProcessor = new NdexQueuedTaskProcessor();

	}
	

	protected String resolveVertexId(VertexFrame vf)
    {
        if (null == vf)
            return null;

        return IdConverter.toJid((ORID)vf.asVertex().getId());
    }

}
