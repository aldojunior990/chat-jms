package com.example.chat_jms.usecases;

import com.example.chat_jms.domain.MessageType;
import com.example.chat_jms.dto.ServerMessageDTO;
import com.example.chat_jms.domain.ConnectedUser;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class SendUserDetails {

    public void execute(WebSocketSession session, ConnectedUser user) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            ServerMessageDTO userDetails = new ServerMessageDTO(
                    MessageType.USER_DETAILS.getValue(),
                    "(" + user.id().toString() + ", " + user.name() + ")"
            );

            String message = objectMapper.writeValueAsString(userDetails);

            session.sendMessage(new TextMessage(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
