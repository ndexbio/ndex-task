package org.ndexbio.task;

import java.util.concurrent.CompletionService;
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
	private   final CompletionService<Integer> taskCompletionService;
	private final ExecutorService taskExecutor;
	
	
	public NdexQueuedTaskProcessor() {
		 taskExecutor = Executors.newFixedThreadPool(MAX_THREADS);
	       this.taskCompletionService =
	           new ExecutorCompletionService<Integer>(taskExecutor);       
	}
	
	private void processITasksQueuedForDeletion() {
		try {
			this.taskService.deleteTasksQueuedForDeletion();
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
			/*
			 * obtain a List of queued tasks and update their status to staged
			 */
			List<ITask> stagedTasks = taskService.stageQueuedITasks();
			if(!stagedTasks.isEmpty()){
				NdexTaskQueueService.INSTANCE.addCollection(stagedTasks);		
				logger.info("The task queue contains" +NdexTaskQueueService.INSTANCE.getTaskQueueSize()
						+" staged tasks");
			
				int threadCount = Math.min(NdexTaskQueueService.INSTANCE.getTaskQueueSize(), 
						MAX_THREADS);
				int startedThreads = 0;
				while( startedThreads < threadCount){
					startedThreads++;
					this.taskCompletionService.submit(new NdexTaskExecutor(startedThreads));
					logger.info("A NdexTaskExecutor thread started");
					
				}
				// monitor submitted jobs until completion
				Integer totalCompletedTasks = 0;
				for(int threadsCompletd=0;threadsCompletd<startedThreads;threadsCompletd++){
			        try {
			            logger.debug("trying to take from Completion service");
			            Future<Integer> result = taskCompletionService.take();
			            totalCompletedTasks += result.get();
			            
			            System.out.println("Thread completed " +result.get() +" tasks" 
			            		+" Total tasks completed = " +totalCompletedTasks);
			            result.cancel(true);
			            
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
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void shutdown() {
		this.taskExecutor.shutdownNow();
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
	

	public static void main(String[] args) {
		
		NdexQueuedTaskProcessor taskProcessor = new NdexQueuedTaskProcessor();
		logger.info(taskProcessor.getClass().getSimpleName() +" invoked");
		try {
			if (taskProcessor.determineActiveTasks() < 1){
				taskProcessor.processQueuedITasks();
			}
			// delete tasks queued for deletion
			taskProcessor.processITasksQueuedForDeletion();
		} catch (NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} finally {
			taskProcessor.shutdown();
		}
		
		logger.info(taskProcessor.getClass().getSimpleName() + " completed.");
	}
	

	protected String resolveVertexId(VertexFrame vf)
    {
        if (null == vf)
            return null;

        return IdConverter.toJid((ORID)vf.asVertex().getId());
    }

}
