package utils;

import java.io.Serializable;

public class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	private MessageType type;
	private String sender;
	private String content;

	//For messages that includes sender
	public Message(MessageType type, String sender, String content) {
		this.type = type;
		this.sender = sender;
		this.content = content;
	}

	//For messages that don't need sender
	public Message(MessageType type, String content) {
		this.type = type;
		this.sender = null;
		this.content = content;
	}

	//GETTERS
	public MessageType getType() {
		return type;
	}

	public String getSender() {
		return sender;
	}

	public String getContent() {
		return content;
	}
}
