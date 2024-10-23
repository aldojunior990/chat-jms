package com.example.chat_jms.dto;

import java.util.UUID;

public record QueueDTO(String type, String content, UUID senderId, UUID receiverId) {
}
