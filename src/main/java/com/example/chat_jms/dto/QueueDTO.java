package com.example.chat_jms.dto;

import java.util.UUID;

public record QueueDTO(String type, String content, UUID sender, UUID receiver) {

    @Override
    public String toString() {
        return "{" +
                "\"type\": \"" + type + "\"," +
                "\"content\": \"" + content + "\"," +
                "\"sender\": \"" + sender + "\"," +
                "\"receiver\": \"" + receiver + "\"" +
                "}";
    }
}
