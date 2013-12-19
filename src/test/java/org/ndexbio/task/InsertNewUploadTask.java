package org.ndexbio.task;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import org.ndexbio.common.exceptions.*;
import org.ndexbio.orientdb.persistence.OrientDBConnectionService;
import org.ndexbio.orientdb.domain.*;
import org.ndexbio.rest.models.*;
import org.ndexbio.service.helpers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.id.ORID;


/*
 * this class is responsible for inserting a new file upload task into the orientdb database
 * it can be invoked directly as an application to simply create the new task for a background
 * processor to deal with or it can be invoked as part of a more comprehensive test.
 */

public class InsertNewUploadTask {
	private  final File sourceFile;
	private LocalDataService ds;
	
	 private static final Logger logger = LoggerFactory.getLogger(InsertNewUploadTask.class);
	
	public InsertNewUploadTask (String fn){
		this.sourceFile = new File(fn);
		this.ds = new LocalDataService();		
	}
	
	private void persistFileUploadTask() {
		try {
			String newFile = this.copyFileToConfigArea();
			Task task = this.generateTask(newFile);
			this.ds.createTask(task);
			
		} catch (IOException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		} catch (NdexException e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private Task generateTask(String newFile) {
		Task task = new Task();
		task.setResource(newFile);
		task.setCreatedDate(new Date());
		task.setStatus(Status.QUEUED);
		
		return task;
	}

	public static void main(String[] args) {
		String testFile = null;
		if(args.length >0 ){
			testFile = args[0];
		} else {
			testFile = "/tmp/tiny_corpus.xbel";
		}
		logger.info("Schedulting " +testFile +" for loading");
		InsertNewUploadTask test = new InsertNewUploadTask(testFile);
		test.persistFileUploadTask();

	}
	
	private String copyFileToConfigArea () throws IOException{
		String newFile = this.resolveFilename();
		Path uploadFile = Paths.get(new File(newFile).toURI());
		Path inputFile = Paths.get(this.sourceFile.toURI());
		logger.info("Copying " +inputFile +" to " +uploadFile);
		
		Files.copy(inputFile, uploadFile, StandardCopyOption.REPLACE_EXISTING);
		return uploadFile.getFileName().toString();
		 
	}
	
	private String resolveFilename() {
		StringBuilder sb = new StringBuilder();
		sb.append(Configuration.getInstance().getProperty(
				Configuration.UPLOADED_NETWORKS_PATH_PROPERTY));
		sb.append(this.sourceFile.getName());
		return sb.toString();
	}
	
	class LocalDataService extends OrientDBConnectionService {
		
		LocalDataService() {
			super();
			
		}
		
		public Task createTask(final Task newTask) throws IllegalArgumentException, NdexException
	    {
	        Preconditions.checkArgument(null!= newTask,"The task to create is empty.");
			
	        
	        final ORID userRid = RidConverter.convertToRid(this.getLoggedInUser().getId());

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
	            task.setType(TaskType.PROCESS_UPLOADED_NETWORK);

	            _orientDbGraph.getBaseGraph().commit();
	            logger.info("file upload task for " + task.getResource() +" created");

	            newTask.setId(RidConverter.convertToJid((ORID) task.asVertex().getId()));
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
			user.setId("C30R2");
			user.setUsername("fcriscuo");
			return user;
			
		}
		
		
	}
	


}
