package org.ndexbio.task.utility;

import org.ndexbio.common.access.NdexDatabase;
import org.ndexbio.common.exceptions.NdexException;
import org.ndexbio.common.exceptions.ObjectNotFoundException;
import org.ndexbio.common.models.dao.orientdb.UserDAO;
import org.ndexbio.model.object.NewUser;
import org.ndexbio.model.object.User;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;

public class DatabaseInitializer {

	public static void main(String[] args) {
		
		NdexDatabase db = null;
		ODatabaseDocumentTx conn = null;
		try {

			db = new NdexDatabase();

		} catch (NdexException e) {
			System.err.println ("Error accurs when initializing Ndex database. " +  
					e.getMessage());
		} finally {
/*			if ( conn != null)
				conn.close(); */
			if ( db != null ) db.close();
		}
	}

	private static void createUserIfnotExist(UserDAO dao, String accountName, String email, String password) throws NdexException {
		try {
			User u = dao.getUserByAccountName(accountName);
			if ( u!= null) return;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new NdexException ("Failed to create new user after creating database. " + e.getMessage());
		} catch ( ObjectNotFoundException e2) {
			
		}
		
		NewUser newUser = new NewUser();
        newUser.setEmailAddress(email);
        newUser.setPassword(password);
        newUser.setAccountName(accountName);
        newUser.setFirstName("");
        newUser.setLastName("");
        dao.createNewUser(newUser);

	}
	
}
