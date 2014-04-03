package org.ndexbio.task;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.ITask;
import org.ndexbio.common.models.data.IUser;
import org.ndexbio.common.models.data.Status;
import org.ndexbio.common.models.data.TaskType;
import org.ndexbio.common.models.object.NdexDataModelService;
import org.ndexbio.common.models.object.Task;
import org.ndexbio.common.models.object.User;
import org.ndexbio.common.persistence.orientdb.OrientDBNoTxConnectionService;
import org.ndexbio.task.event.NdexNetworkState;
import org.ndexbio.task.event.NdexTaskEventHandler;
import org.ndexbio.task.service.NdexJVMDataModelService;
import org.ndexbio.xbel.exporter.XbelNetworkExporter;
import org.ndexbio.xbel.exporter.XbelNetworkExporter.XbelMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.id.ORID;

/*
 * Test application to insert network export tasks into the Task type
 */
public class TestXbelExportTask {

	
	private static final Logger logger = LoggerFactory.getLogger(TestXbelExportTask.class);
	private final String[] networkIds;
	private final LocalDataService ds;
	
	public TestXbelExportTask (String[] ids){
		this.networkIds = ids;
		this.ds = new LocalDataService();
		this.insertExportTasks();
		
	}
	
	
	public static void main(String[] args) throws IOException {
		//String networkId = "C25R732"; // is for large corpus
		String[] ids = new String[]{"C25R732"}; // is for small corpus
		TestXbelExportTask test = new TestXbelExportTask(ids);
		//add shutdown hook
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				System.out.println("TextXbelExportTask completed.");
			}
		});
	}
	
	
	
	private void insertExportTasks(){
		for (String id : this.networkIds){
			
			try {
				Task task = this.generateTask(id);
				this.ds.createTask(task);
				logger.info("netwok upload task " +task.getId() +" queued in database");
			} catch (IllegalArgumentException | NdexException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			  
		}
	}
	
	private Task generateTask(String networkId) {
		Task task = new Task();
		task.setResource(networkId);
		task.setCreatedDate(new Date());
		task.setStatus(Status.QUEUED);
		
		return task;
	}
	
class LocalDataService extends OrientDBNoTxConnectionService {
		
		LocalDataService() {
			super();
			
		}
		
		public Task createTask(final Task newTask) throws IllegalArgumentException, NdexException
	    {
	        Preconditions.checkArgument(null!= newTask,"The task to create is empty.");
			
	        
	        final ORID userRid = IdConverter.toRid(this.getLoggedInUser().getId());

	        try
	        {
	            setupDatabase();
	            
	            final IUser taskOwner = _orientDbGraph.getVertex(userRid, IUser.class);
	            
	            final ITask task = _orientDbGraph.addVertex("class:task", ITask.class);
	            task.setOwner(taskOwner);
	            task.setStatus(newTask.getStatus());
	            task.setStartTime(newTask.getCreatedDate());
	            task.setResource(newTask.getResource());
	            task.setProgress(0);
	            task.setType(TaskType.EXPORT_NETWORK_TO_FILE);
	            _orientDbGraph.getBaseGraph().commit();
	            logger.info("network export task for " + task.getResource() +" created");

	            newTask.setId(IdConverter.toJid((ORID) task.asVertex().getId()));
	            return newTask;
	        }
	        catch (Exception e)
	        {
	            logger.error("Failed to create a task : " , e);
	            _orientDbGraph.getBaseGraph().rollback(); 
	            throw new NdexException("Failed to create a task.");
	        }
	        finally
	        {
	            teardownDatabase();
	        }
	    }

		private User getLoggedInUser() {
			User user = new User();
			user.setId("C31R1");
			user.setUsername("biologist2");
			return user;
			
		}
		
		
	}
	

	
}
