package com.example.chat_jms.dto;

import java.util.UUID;

public record TopicDTO(String type, String content, UUID sender) {
    @Override
    public String toString() {
        return "{" +
                "\"type\": \"" + type + "\"," +
                "\"content\": \"" + content + "\"," +
                "\"sender\": \"" + sender + "\"" +
                "}";
    }
}
