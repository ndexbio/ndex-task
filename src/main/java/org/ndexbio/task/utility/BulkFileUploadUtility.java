package org.ndexbio.task.utility;


import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import org.ndexbio.common.exceptions.*;
import org.ndexbio.common.helpers.IdConverter;
import org.ndexbio.common.models.data.*;
import org.ndexbio.common.models.object.*;
import org.ndexbio.common.persistence.orientdb.OrientDBNoTxConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.orientechnologies.orient.core.id.ORID;


/*
 * Represents a utility application responsible for preparing a set of network files 
 * for uploading into Ndex. The files must be an acceptable format (i.e. xbel, sif, xls, xlsx)
 * The input parameter is a directory location. All regular files within the specified directory 
 * are processed
 */
public class BulkFileUploadUtility {
	
	private final Path uploadDir;
	private final LocalDataService ds;
	
	public BulkFileUploadUtility(String dir){
		this.uploadDir = Paths.get(dir);
		this.ds = new LocalDataService();
	}

	private static final String DEFAULT_DIRECTORY = "/tmp/ndex/bulk";
	private static final Logger logger = LoggerFactory.getLogger(BulkFileUploadUtility.class);
	private static final String NETWORK_UPLOAD_PATH = "/opt/ndex/uploaded-networks/";
	
	public static void main(String[] args) {
		String dir = DEFAULT_DIRECTORY;
		if (args.length >0){
			dir = args[0];
		}
		logger.info("Processing network files in  " +dir);
		BulkFileUploadUtility util = new BulkFileUploadUtility(dir);
		util.processFiles();

	}
	
	private void processFiles() {
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(this.uploadDir)) {
            for (Path path : directoryStream) {
              logger.info("Processing file " +path.toString());
              String destFile = this.resolveFilename(path);
              Path destPath = Paths.get(destFile);
              Files.copy(path, destPath, StandardCopyOption.REPLACE_EXISTING);
              logger.info("Copied " +path.toString() +" to " +destPath.toString() );
              Task task = this.generateTask(destPath.toString());
  			  this.ds.createTask(task);
  			  logger.info("file upload task " +task.getId() +" queued in database");
            }
        } catch (IOException | IllegalArgumentException | NdexException e) {
        	logger.error(e.getMessage());
        }
	}
	
	private Task generateTask(String newFile) {
		Task task = new Task();
		task.setResource(newFile);
		task.setCreatedDate(new Date());
		task.setStatus(Status.QUEUED);
		
		return task;
	}
	
	private String resolveFilename(Path sourcePath) {
		StringBuilder sb = new StringBuilder();
		sb.append(NETWORK_UPLOAD_PATH);
		sb.append(sourcePath.getFileName());
		return sb.toString();
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
	            task.setType(TaskType.PROCESS_UPLOADED_NETWORK);

	            _orientDbGraph.getBaseGraph().commit();
	            logger.info("file upload task for " + task.getResource() +" created");

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
			user.setId("C30R2");
			user.setUsername("fjcriscuolo");
			return user;
			
		}
		
		
	}
	

}
