package com.tah.commapp;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class TalkDBUtil {

	private static final String CONVERSATIONS_COLLECTION = "convos";
	private static Mongo mongo;

	static {
		try {
			mongo = new Mongo("localhost", 27017);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (MongoException e) {
			e.printStackTrace();
		}
	}

	public static DB getDB() {
		// boolean auth = db.authenticate(myUserName, myPassword);
		return mongo.getDB("tahdb");
	}
	
	
	/* ------------- Talker --------------- */
	public static void talkerConnected(int tid, String talkerId, String talkerName) {
		if (isEmptyTalk(tid)) {
			saveTalkAction(tid, "started");
		}
		
		saveConnectionAction(tid, talkerId, "joined");
		updateLiveTalkers(tid, talkerId, talkerName, true);
	}
	
	public static void talkerDisconnected(int tid, String talkerId, String talkerName) {
		saveConnectionAction(tid, talkerId, "left");
		updateLiveTalkers(tid, talkerId, talkerName, false);
		
		if (isEmptyTalk(tid)) {
			saveTalkAction(tid, "finished");
		}
	}
	
	//"talkers" - array of currently live talkers in this conversation  - [user id, userName]
	private static void updateLiveTalkers(int tid, String talkerId, 
			String talkerName, boolean connected) {
		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBObject talkerDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("uname", talkerName)
			.get();
		
		DBObject tidDBObject = new BasicDBObject("tid", tid);
		String operation = "$pull"; //for disconnected
		if (connected) {
			operation = "$push";
		}
		convosColl.update(tidDBObject, 
				new BasicDBObject(operation, new BasicDBObject("talkers", talkerDBObject)));
	}
	
	//"actions" - array of different actions in this talk - user joined, left, etc
	private static void saveConnectionAction(int tid, String talkerId, String action) {
		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBObject actionDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("action", action)
			.add("time", new Date())
			.get();
		
		DBObject tidDBObject = new BasicDBObject("tid", tid);
		convosColl.update(tidDBObject, 
				new BasicDBObject("$push", new BasicDBObject("actions", actionDBObject)));
	}
	
	/* --------------- Conversation ----------------- */
	public static void saveMessage(int tid, String talkerId, String text) {
		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBObject commentDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("text", text)
			.add("cr_date", new Date())
			.get();
		
		DBObject tidDBObject = new BasicDBObject("tid", tid);
		convosColl.update(tidDBObject, 
				new BasicDBObject("$push", new BasicDBObject("messages", commentDBObject)));
	}
	
	public static List<Message> loadMessages(int tid) {
		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("tid", tid)
			.get();
			
		DBObject convoDBObject = convosColl.findOne(query, new BasicDBObject("messages", ""));
		BasicDBList messagesDBList = (BasicDBList)convoDBObject.get("messages");
		
		List<Message> messagesList = new ArrayList<Message>();
		if (messagesDBList == null) {
			return messagesList;
		}
		
		for (Object obj : messagesDBList) {
			DBObject messageDBObject = (DBObject)obj;
			Message message = new Message();
			message.setText((String)messageDBObject.get("text"));
			message.setTime((Date)messageDBObject.get("time"));
			DBObject fromTalker = ((DBRef)messageDBObject.get("uid")).fetch();
			message.setTalker((String)fromTalker.get("uname"));
			
			messagesList.add(message);
		}
		return messagesList;
	}
	
	private static boolean isEmptyTalk(int tid) {
		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("tid", tid)
			.get();
			
		DBObject talkDBObject = convosColl.findOne(query, new BasicDBObject("talkers", ""));
		BasicDBList liveTalkersList = (BasicDBList)talkDBObject.get("talkers");
		if (liveTalkersList == null || liveTalkersList.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	private static void saveTalkAction(int tid, String action) {
		DBCollection convosColl = getDB().getCollection(CONVERSATIONS_COLLECTION);
		
		DBObject actionDBObject = BasicDBObjectBuilder.start()
			.add("action", action)
			.add("time", new Date())
			.get();
		
		DBObject tidDBObject = new BasicDBObject("tid", tid);
		convosColl.update(tidDBObject, 
				new BasicDBObject("$push", new BasicDBObject("topic_actions", actionDBObject)));
	}
	
	public static void main(String[] args) {
		TalkDBUtil.talkerConnected(43, "4c2cb43160adf3055c97d061", "kangaroo");
//		TalkDBUtil.talkerConnected(1, "4c400cb43a10f305774734e8", "wewewe");
//		TalkDBUtil.talkerDisconnected(1, "4c2cb43160adf3055c97d061", "kangaroo");
//		TalkDBUtil.talkerDisconnected(1, "4c400cb43a10f305774734e8", "wewewe");
//		TalkDBUtil.saveMessage(1, "4c2cb43160adf3055c97d061", "Second!!");
		
//		TalkDBUtil.updateLiveTalkers("4c31e7a14e08f30586b71a50", "4c2cb43160adf3055c97d061", "kangaroo", false);
		
//		System.out.println(TalkDBUtil.isEmptyTopic("4c31e7a14e08f30586b71a50"));
		
//		System.out.println(TalkDBUtil.loadMessages(1));
		
		System.out.println("finished");
	}
}
