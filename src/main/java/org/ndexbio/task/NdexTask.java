package org.ndexbio.task;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.orientdb.domain.Status;
import org.ndexbio.orientdb.persistence.NdexTaskService;
import org.ndexbio.rest.models.Task;

public abstract class NdexTask implements Runnable {
	
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

	@Override
	public abstract void run() ;

}
