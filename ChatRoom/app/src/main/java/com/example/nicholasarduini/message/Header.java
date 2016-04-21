package com.example.nicholasarduini.message;

import java.io.Serializable;

public class Header implements Serializable {

	private static final long serialVersionUID = -7729816603167728273L;
	public String sender;
	public String receiver;
	public String type;
	
	public Header() {
		sender = Message.DEFAULT_SENDER;
		receiver = Message.DEFAULT_RECEIVER;
		type = Message.DEFAULT_TYPE;
	}

	public Header(String sender, String receiver, String type){
		this.sender = sender;
		this.receiver = receiver;
		this.type = type;
	}
}

