package org.ndexbio.task;

import java.util.concurrent.Callable;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.model.object.Task;
import org.ndexbio.model.object.Status;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;

import com.orientechnologies.orient.core.id.ORID;

public abstract class NdexTask implements Callable<Task> {
	
	private final String taskId;
	private final String userId;
	private final NdexTaskService taskService;
	private  Task task;
	
	
	public NdexTask(String aTaskId) throws IllegalArgumentException, SecurityException, NdexException{
		this.taskId = aTaskId;
		this.taskService = new NdexTaskService();
		this.task = taskService.getTask(taskId);
		this.userId = (String) this.task.getOwner().asVertex().getId();
	}
	
	public NdexTask(Task itask) {
		this.taskService = new NdexTaskService();
		this.task = itask;
		this.taskId = this.resolveVertexId(itask);
		this.userId = this.resolveVertexId(this.task.getOwner());
	}
	protected String getUserId() { return this.userId;}
	
	protected String getTaskId() {return this.taskId; }
	
	protected Task getTask() { return this.task;}
	
	protected final void startTask() throws IllegalArgumentException, 
		ObjectNotFoundException, SecurityException, NdexException{
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

	public abstract Task call() throws Exception;
	
	protected String resolveVertexId(VertexFrame vf)
    {
        if (null == vf)
            return null;

        return IdConverter.toJid((ORID)vf.asVertex().getId());
    }
	
	

}
