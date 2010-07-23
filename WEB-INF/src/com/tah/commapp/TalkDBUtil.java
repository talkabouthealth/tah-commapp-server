package com.tah.commapp;

import java.net.UnknownHostException;
import java.util.Date;

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
	public static void talkerConnected(String topicId, String talkerId, String talkerName) {
		if (isEmptyTopic(topicId)) {
			saveTopicAction(topicId, "started");
		}
		
		saveConnectionAction(topicId, talkerId, "joined");
		updateLiveTalkers(topicId, talkerId, talkerName, true);
	}
	
	public static void talkerDisconnected(String topicId, String talkerId, String talkerName) {
		saveConnectionAction(topicId, talkerId, "left");
		updateLiveTalkers(topicId, talkerId, talkerName, false);
		
		if (isEmptyTopic(topicId)) {
			saveTopicAction(topicId, "finished");
		}
	}
	
	//"talkers" - array of currently live talkers in this conversation  - [user id, userName]
	private static void updateLiveTalkers(String topicId, String talkerId, 
			String talkerName, boolean connected) {
		DBCollection topicsColl = getDB().getCollection("topics");
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBObject talkerDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("uname", talkerName)
			.get();
		
		DBObject tidDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		String operation = "$pull"; //for disconnected
		if (connected) {
			operation = "$push";
		}
		topicsColl.update(tidDBObject, 
				new BasicDBObject(operation, new BasicDBObject("talkers", talkerDBObject)));
	}
	
	//"actions" - array of different actions in this talk - user joined, left, etc
	private static void saveConnectionAction(String topicId, String talkerId, String action) {
		DBCollection topicsColl = getDB().getCollection("topics");
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBObject actionDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("action", action)
			.add("time", new Date())
			.get();
		
		DBObject tidDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		topicsColl.update(tidDBObject, 
				new BasicDBObject("$push", new BasicDBObject("actions", actionDBObject)));
	}
	
	/* --------------- Conversation/Topic ----------------- */
	public static void saveComment(String topicId, String talkerId, String talkerName, String text) {
		DBCollection topicsColl = getDB().getCollection("topics");
		
		DBRef talkerRef = new DBRef(getDB(), "talkers", new ObjectId(talkerId));
		DBObject commentDBObject = BasicDBObjectBuilder.start()
			.add("uid", talkerRef)
			.add("text", text)
			.add("cr_date", new Date())
			.get();
		
		DBObject tidDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		topicsColl.update(tidDBObject, 
				new BasicDBObject("$push", new BasicDBObject("comments", commentDBObject)));
	}
	
	private static boolean isEmptyTopic(String topicId) {
		DBCollection topicsColl = getDB().getCollection("topics");
		
		DBObject query = BasicDBObjectBuilder.start()
			.add("_id", new ObjectId(topicId))
			.get();
			
		DBObject topicDBObject = topicsColl.findOne(query, new BasicDBObject("talkers", ""));
		BasicDBList liveTalkersList = (BasicDBList)topicDBObject.get("talkers");
		if (liveTalkersList == null || liveTalkersList.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	private static void saveTopicAction(String topicId, String action) {
		DBCollection topicsColl = getDB().getCollection("topics");
		
		DBObject actionDBObject = BasicDBObjectBuilder.start()
			.add("action", action)
			.add("time", new Date())
			.get();
		
		DBObject tidDBObject = new BasicDBObject("_id", new ObjectId(topicId));
		topicsColl.update(tidDBObject, 
				new BasicDBObject("$push", new BasicDBObject("topic_actions", actionDBObject)));
	}
	
	public static void main(String[] args) {
//		TalkDBUtil.talkerConnected("4c31e7a14e08f30586b71a50", "4c2cb43160adf3055c97d061", "kangaroo");
//		TalkDBUtil.talkerDisconnected("4c31e7a14e08f30586b71a50", "4c2cb43160adf3055c97d061", "kangaroo");
//		TalkDBUtil.talkerDisconnected("4c31e7a14e08f30586b71a50", "4c400cb43a10f305774734e8", "wewewe");
//		TalkDBUtil.saveComment("4c31e7a14e08f30586b71a50", "4c2cb43160adf3055c97d061", "First test comment :)");
		
//		TalkDBUtil.updateLiveTalkers("4c31e7a14e08f30586b71a50", "4c2cb43160adf3055c97d061", "kangaroo", false);
		
//		System.out.println(TalkDBUtil.isEmptyTopic("4c31e7a14e08f30586b71a50"));
		
		System.out.println("finished");
	}
}
