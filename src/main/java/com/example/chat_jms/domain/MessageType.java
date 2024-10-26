package com.example.chat_jms.domain;

public enum MessageType {
    USER_DETAILS("user-details"),
    TOPIC("topic"),
    QUEUE("queue"),
    USERS_LIST("users-list");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}