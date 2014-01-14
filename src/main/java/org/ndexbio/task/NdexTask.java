package org.ndexbio.task;

import java.util.concurrent.Callable;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.data.ITask;
import org.ndexbio.common.models.data.Status;

import org.ndexbio.common.persistence.orientdb.NdexTaskService;



public abstract class NdexTask implements Callable<ITask> {
	
	private final String taskId;
	
	private final NdexTaskService taskService;
	private  ITask task;
	
	
	public NdexTask(String aTaskId) throws IllegalArgumentException, SecurityException, NdexException{
		this.taskId = aTaskId;
		this.taskService = new NdexTaskService();
		this.task = taskService.getITask(taskId);
	}
	
	protected String getTaskId() {return this.taskId; }
	
	protected ITask getTask() { return this.task;}
	
	protected final void startTask() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException{
		this.updateTaskStatus(Status.PROCESSING);
	}
	
	/*
	 * update the actual itask in the task service which is responsible for database connections
	 * refresh the itask instancce to reflect the updated status
	 * do not set the status directly since the database connection may be closed
	 * 
	 */
	protected final void updateTaskStatus(Status status) throws IllegalArgumentException, 
		ObjectNotFoundException, SecurityException, NdexException{
		this.task = this.taskService.updateTaskStatus(status, this.getTaskId());
	}

	public abstract ITask call() throws Exception;
	

}
