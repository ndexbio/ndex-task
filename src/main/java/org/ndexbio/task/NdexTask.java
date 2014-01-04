package org.ndexbio.task;

import java.util.concurrent.Callable;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.data.Status;
import org.ndexbio.common.models.object.Task;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;



public abstract class NdexTask implements Callable<Task> {
	
	private final String taskId;
	
	private final NdexTaskService taskService;
	private final Task task;
	
	
	public NdexTask(String aTaskId) throws IllegalArgumentException, SecurityException, NdexException{
		this.taskId = aTaskId;
		this.taskService = new NdexTaskService();
		this.task = taskService.getITask(taskId);
	}
	
	protected String getTaskId() {return this.taskId; }
	
	protected Task getTask() { return this.task;}
	
	protected final void startTask() throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException{
		this.updateTaskStatus(Status.PROCESSING);
	}
	
	protected final void updateTaskStatus(Status status) throws IllegalArgumentException, ObjectNotFoundException, SecurityException, NdexException{
		this.getTask().setStatus(status);
		this.taskService.updateTask(this.getTask());
	}

	public abstract Task call() throws Exception;
	

}
