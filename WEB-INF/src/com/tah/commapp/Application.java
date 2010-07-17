package com.tah.commapp;

import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.red5.server.api.IConnection;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;

import org.red5.server.api.IScope;
import org.red5.server.api.service.*;
import org.red5.server.api.so.ISharedObject;

public class Application extends MultiThreadedApplicationAdapter
{
    private static final Log log = LogFactory.getLog( Application.class );
    
    public boolean appStart ( )
    {
    	log.error("***App Start***");
        return true;
    }

    public void appStop ( )
    {
        log.error( "Red5First.appStop" );
    }

    public boolean connect( IConnection conn , IScope scope, Object[] params )
    {
    	log.error( "scope: " + scope.toString());
    	log.error( "conn: " + conn.toString());
    	log.error( "client: " + conn.getClient().toString());
    	
    	String userid = params[0].toString();
		String username = params[1].toString();
		String topicid = params[2].toString();
		String topic = params[3].toString();
		
		log.error( "params: " + userid + username + topicid + topic);
    	
		ISharedObject so = getSharedObject(scope, "talkerListSO-" + topicid); 
		ArrayList<String[]> talkerListArray = new ArrayList<String[]>();
	    
		if (so == null) {
			log.error( "Shared object is null, creating object and adding username");
	    	
    	    // create shared object
			createSharedObject(scope, "talkerListSO-" + topicid, false);
			so = getSharedObject(scope, "talkerListSO-" + topicid); 
			
			talkerListArray.add(new String[] {conn.getClient().getId(), username});
			so.setAttribute("talkerListAC", talkerListArray);
			
		} else {
			log.error( "Adding username to shared object array");
	    	
			talkerListArray = (ArrayList<String[]>)so.getAttribute("talkerListAC");
			talkerListArray.add(new String[] {conn.getClient().getId(), username});
			
			so.setAttribute("talkerListAC", talkerListArray);
		}
		
		// add user to database to track live conversations - userid, topicid
		
        return true;
    }

    /*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.ApplicationAdapter#disconnect(org.red5.server.api.IConnection, org.red5.server.api.IScope)
	 * disconnect an user form the chat and notify all others users 
	 */
	public void disconnect(IConnection conn, IScope scope) {       
		
		// get shared object
		ISharedObject so = getSharedObject(scope, "talkerListSO-" + scope.getName());
		ArrayList<String[]> talkerListArray = new ArrayList<String[]>();
	    talkerListArray = (ArrayList<String[]>)so.getAttribute("talkerListAC");
		
	    for (int i = 0; i < talkerListArray.size(); i++) {
	    	if((talkerListArray.get(i))[0] == conn.getClient().getId()){
	    		// remove user from shared object
	    	    talkerListArray.remove(i);
	    		break;
	    	}
	    }
	    // set array on shared object
	    so.setAttribute("talkerListAC", talkerListArray);
		
	    // remove user from database
		
	}	
	
    /*
	 * send a public message msg from from_pseudo to ALL users
	 */
	public void send_msgtoroom(String username, String topicid, String msg) {
		log.error("Server msg to room");
		log.error("Scope: " + scope.toString());
        ServiceUtils.invokeOnAllConnections (getChildScope(topicid), "receivePublicMsg", new Object[] {username, msg} );
        // store message in database - user id, topicid, message
	}
}