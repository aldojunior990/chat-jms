package com.example.chat_jms.controller;

import com.example.chat_jms.domain.ChatOutput;
import com.example.chat_jms.domain.ChatInput;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class BroadcastChatController {

    @MessageMapping("/sendBroadcastMessage")
    @SendTo("/topic/broadcast")
    public ChatOutput sendBroadcastMessage(ChatInput input) {
        return new ChatOutput(input.user() + ": " + input.message()); // Envia a mensagem para todos os inscritos no tópico
    }
}
