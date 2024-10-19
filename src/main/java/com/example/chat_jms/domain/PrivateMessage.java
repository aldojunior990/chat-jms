package com.example.chat_jms.domain;

public record PrivateMessage(
        String recipient, String content) {
}