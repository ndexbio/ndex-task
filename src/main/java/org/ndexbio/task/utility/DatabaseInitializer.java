package org.ndexbio.task.utility;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.object.NewUser;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class DatabaseInitializer {

	public static void main(String[] args) {
		
		NdexDatabase db = null;
		ODatabaseDocumentTx conn = null;
		try {

			db = new NdexDatabase();
			conn = db.getAConnection();
			UserDAO dao = new UserDAO(conn);

			NewUser newUser = new NewUser();
	        newUser.setEmailAddress("reactomeadmin@ndexbio.org");
	        newUser.setPassword("reactome-321");
	        newUser.setAccountName("reactomeadmin");
	        newUser.setFirstName("");
	        newUser.setLastName("");
	        dao.createNewUser(newUser);

	        newUser.setEmailAddress("pidadmin@ndexbio.org");
	        newUser.setPassword("pid-321");
	        newUser.setAccountName("pidadmin");
	        newUser.setFirstName("");
	        newUser.setLastName("");
	        dao.createNewUser(newUser);
	        
		} catch (NdexException e) {
			System.err.println ("Error accurs when initializing Ndex database. " +  
					e.getMessage());
		} finally {
			if ( conn != null)
				conn.close();
			if ( db != null ) db.close();
		}
	}

}
