package com.tah.commapp;

import java.util.Date;

public class Message {
	
	private String text;
	//username
	private String talker;
	private Date time;
	
	@Override
	public String toString() {
		return talker+": "+text;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getTalker() {
		return talker;
	}
	public void setTalker(String talker) {
		this.talker = talker;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}

}
