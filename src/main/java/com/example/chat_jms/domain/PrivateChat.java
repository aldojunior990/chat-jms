package com.example.chat_jms.domain;

import jakarta.jms.Queue;

import java.util.Set;

public record PrivateChat(String chatID, Set<ConnectedUser> participants) {
}
