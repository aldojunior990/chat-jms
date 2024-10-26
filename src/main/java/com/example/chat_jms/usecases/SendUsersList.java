package com.example.chat_jms.usecases;

import com.example.chat_jms.domain.MessageType;
import com.example.chat_jms.dto.ServerMessageDTO;
import com.example.chat_jms.infra.UsersRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class SendUsersList {

    @Autowired
    private UsersRepository usersRepository;

    public void execute() {
        ServerMessageDTO activeUsersMessage = new ServerMessageDTO(
                MessageType.USERS_LIST.getValue(),
                "(1, topic), "+ usersRepository.getConnectedUsersAsString());

        List<WebSocketSession> sessions = usersRepository.getAllSessions();

        synchronized (sessions) {
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen()) {
                        ObjectMapper objectMapper = new ObjectMapper();
                        String message = objectMapper.writeValueAsString(activeUsersMessage);
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }
}
