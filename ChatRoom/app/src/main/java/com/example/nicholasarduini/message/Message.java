package com.example.nicholasarduini.message;

import java.io.Serializable;

public class Message implements Serializable {

	//message types
	public static final String DATA = "data";
	public static final String LOGIN = "login";
	public static final String LOGOUT = "logout";
	public static final String USERSLIST = "users list";

	//message receivers
	public static final String EVERYONE_RECEIVER = "Everyone";

	//message senders
	public static final String SERVER_SENDER = "Server";

	//message strings
	public static final String HAS_DISCONNECTED = "has disconnected";
	public static final String IS_HERE = "is here";

	//default header fields
	public static final String DEFAULT_SENDER = "unknown";
	public static final String DEFAULT_RECEIVER = "unknown";
    public static final String DEFAULT_TYPE = "unknown";

	
	private static final long serialVersionUID = 6394396411894185136L;
	public Header header;
	public Body body;
	
	public Message() {
		header = new Header();
		body = new Body();
	}

	public Message(String sender, String receiver, String type, String data){
		header = new Header(sender, receiver, type);
		body = new Body();
		body.addField(type, data);
	}
	
	public String toString(){
		return body.toString();
	}
}
