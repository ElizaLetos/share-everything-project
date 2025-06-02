package com.example.share_everything_project;

public class Message {
    private String id;
    private String sender;
    private String receiver;
    private String content;
    private String type;
    private long timestamp;
    private String createdAt;

    public Message(String sender, String receiver, String content, String type, long timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
    }

    public Message(String id, String sender, String receiver, String content, String type, long timestamp, String createdAt) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getContent() {
        return content;
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getCreatedAt() {
        return createdAt;
    }
} 