package com.example.chat_jms.dto;

import java.util.UUID;

public record UsersDTO(String type, UUID id, String content) {}
