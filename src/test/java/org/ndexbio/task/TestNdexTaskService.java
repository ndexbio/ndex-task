package org.ndexbio.task;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.persistence.orientdb.NdexTaskService;

public class TestNdexTaskService {

	private final NdexTaskService service;
	
	public TestNdexTaskService() throws NdexException {
		this.service = new NdexTaskService();
	}
	
	private void performTests() {
		this.processTasksQueuedForDeletion();
		
	}
	private void processTasksQueuedForDeletion(){
		try {
			service.deleteTasksQueuedForDeletion();
		} catch (NdexException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws NdexException {
		TestNdexTaskService test = new TestNdexTaskService();
		test.performTests();

	}

}
