package org.ndexbio.task;

import java.util.concurrent.TimeUnit;

import org.ndexbio.common.persistence.orientdb.NdexTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractScheduledService;

/*
 * Represents a scheduled task that runs  on a periodic basis to scan for
 * database Task entries that have been marked for deletion
 */

public class TaskDeletionService extends AbstractScheduledService {
	
	private static final Logger logger = LoggerFactory.getLogger(TaskDeletionService.class);
	private final NdexTaskService ndexService = new NdexTaskService();
	
	protected void startup() {
		logger.info("TaskDeletionService started");
	}
	
	/*
	 * This task should run on a continuous basis so stopping it is an error
	 */
	protected void shutdown() {
		logger.error("TaskDeletionService stopped");
	}

	/*
	 * scan the Tasks for those marked for deletion
	 */
	@Override
	protected void runOneIteration() throws Exception {
		this.ndexService.deleteTasksQueuedForDeletion();

	}
	

	/*
	 * schedule a scan for every minute
	 */
	@Override
	protected Scheduler scheduler() {
		return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.MINUTES);
	}

}
