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
    
    public boolean connect( IConnection conn , IScope scope, Object[] params )
    {
		String userid = params[0].toString();
		String username = params[1].toString();
		String topicid = params[2].toString();
		String topic = params[3].toString();
			
		ISharedObject so = getSharedObject(scope, "talkerListSO-" + topicid); 
		ArrayList<String[]> talkerListArray = new ArrayList<String[]>();
		    
		if (so == null) {
			//log.error( "Shared object is null, creating object and adding username");
		   	
	        // create shared object
			createSharedObject(scope, "talkerListSO-" + topicid, false);
			so = getSharedObject(scope, "talkerListSO-" + topicid); 
			
			talkerListArray.add(new String[] {conn.getClient().getId(), username});
			so.setAttribute("talkerListAC", talkerListArray);
			
		} else {
			//log.error( "Adding username to shared object array");
		   	
			talkerListArray = (ArrayList<String[]>)so.getAttribute("talkerListAC");
			talkerListArray.add(new String[] {conn.getClient().getId(), username});
			
			so.setAttribute("talkerListAC", talkerListArray);
		}
	    	
		/*********************************************************************
		*
		*Target: new SharedObject for video list
		* Add by: Situ
		****************/
		
		so = getSharedObject(scope, "videoListSO-"+ topicid);
		if(so == null){
			//create shared object
			createSharedObject(scope, "videoListSO-"+ topicid, false);
		}
		/********************************************************************/
		// add user to database to track live conversations - userid, topicid
		if (scope.getDepth() == 1) {
		   	TalkDBUtil.talkerConnected(Integer.parseInt(topicid), userid, username);
		}	
		conn.setAttribute("userid", userid);
		conn.setAttribute("username", username);
		conn.setAttribute("topicid", topicid);
		
        return true;
    }
    /*
	 * (non-Javadoc)
	 * @see org.red5.server.adapter.ApplicationAdapter#disconnect(org.red5.server.api.IConnection, org.red5.server.api.IScope)
	 * disconnect an user form the chat and notify all others users 
	 */
	public void disconnect(IConnection conn, IScope scope) {       
		// remove user from database
	    if (scope.getDepth() == 1) {
	    	TalkDBUtil.talkerDisconnected(Integer.parseInt((String)conn.getAttribute("topicid")), (String)conn.getAttribute("userid"), (String)conn.getAttribute("username"));
	    }
	    
		// get shared object
		ISharedObject so = getSharedObject(scope, "talkerListSO-" + scope.getName());
		if(so != null){
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
			/*********************************************************************
			* Target: Remove the user from video list before disconnect
			* Added by: Situ
			******************/
		   removeVideoList(conn.getClient().getId(), scope.getName());
			/*********************************************************************/
		}
	}	
	
    /*
	 * send a public message msg from from_pseudo to ALL users
	 */
	public void send_msgtoroom(String userid, String username, String topicid, String msg) {
		ServiceUtils.invokeOnAllConnections (getChildScope(topicid), "receivePublicMsg", new Object[] {username, msg} );
        
		// store message in database - user id, topicid, message
		TalkDBUtil.saveComment(Integer.parseInt(topicid), userid, msg);
	}
	
	/**************************************************************
	 *            Update videoList: insert new client ID 
	 **************************************************************/
	public void addVideoList(String clientID, String topicID){
		//log.error("addVideoList at server side called ...");
		IScope scope = getChildScope(topicID);
		String shareObjectName = "videoListSO-"+topicID;
		String videoListName = "videoListAC";
		ArrayList<String> videoList = null;
		ISharedObject so = getSharedObject(scope, shareObjectName); 
		
		videoList = (ArrayList<String>) so.getAttribute(videoListName);
		if(videoList == null){
			videoList = new ArrayList<String>();
			so.setAttribute(videoListName, videoList);
		}
		videoList.add(clientID);
		so.setAttribute(videoListName, videoList);
	}
	
	public void removeVideoList(String clientID, String topicID){
		IScope scope = getChildScope(topicID);
		String shareObjectName = "videoListSO-"+topicID;
		String videoListName = "videoListAC";
		ArrayList<String> videoList = new ArrayList<String>();
	    ISharedObject so = getSharedObject(scope, shareObjectName); 
				
		videoList = (ArrayList<String>) so.getAttribute(videoListName);
		videoList.remove(clientID);
	    so.setAttribute(videoListName, videoList);
	}
	
}