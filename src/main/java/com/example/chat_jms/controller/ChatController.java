package com.example.chat_jms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.chat_jms.domain.PrivateMessage;

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat")
    public void sendMessage(String message) {
        messagingTemplate.convertAndSend("/topic/messages", message);
    }

    @MessageMapping("/chat-private")
    public void sendPrivateMessage(PrivateMessage privateMessage) {
        messagingTemplate.convertAndSendToUser(
                privateMessage.recipient(),
                "/queue/messages",
                privateMessage.content());
    }
}
