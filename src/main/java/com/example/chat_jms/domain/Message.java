package com.example.chat_jms.domain;

public record Message(
        String type, String content, String sender, String receiver
) {
}