package com.example.chat_jms.domain;

import jakarta.jms.Queue;
import org.springframework.web.socket.WebSocketSession;


import java.util.UUID;

public record ConnectedUser(UUID id,
                            String name,
                            Queue queue,
                            WebSocketSession wsSession) {
}
