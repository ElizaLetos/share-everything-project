package com.example.share_everything_project;

public class Message {
    public String sender;
    public String content;
    public String type; // "text" sau "file"
    public long timestamp;

    public Message() {} // necesar pentru Firebase

    public Message(String sender, String content, String type, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
    }
}

