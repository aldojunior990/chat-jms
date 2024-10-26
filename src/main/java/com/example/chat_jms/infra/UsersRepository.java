package com.example.chat_jms.infra;

import com.example.chat_jms.domain.ConnectedUser;
import com.example.chat_jms.domain.PrivateChat;
import org.springframework.stereotype.Repository;
import org.springframework.web.socket.WebSocketSession;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
public class UsersRepository {

    private final Set<ConnectedUser> connectedUsers = Collections.synchronizedSet(new HashSet<>());

    private final Set<PrivateChat> openPrivateChats = Collections.synchronizedSet(new HashSet<>());

    public void add(ConnectedUser connectedUser) {
        this.connectedUsers.add(connectedUser);
    }

    public void removeBySession(WebSocketSession session) {
        connectedUsers.removeIf(user -> user.wsSession().equals(session));
        openPrivateChats.removeIf(chat ->
                chat.participants().stream().anyMatch(user -> user.wsSession().equals(session))
        );
    }

    public void updateOpenChats(PrivateChat privateChat) {
        openPrivateChats.add(privateChat);
    }

    public PrivateChat getPrivateChatById(String id) {
        PrivateChat privateChat = null;

        for (PrivateChat it : openPrivateChats) {
            if (Objects.equals(it.chatID(), id))
                privateChat = it;
        }

        return privateChat;
    }


    public List<WebSocketSession> getAllSessions() {
        return connectedUsers.stream()
                .map(ConnectedUser::wsSession)
                .collect(Collectors.toList());
    }

    public String getConnectedUsersAsString() {
        return connectedUsers.stream()
                .map(user -> "(" + user.id() + ", " + user.name() + ")")
                .collect(Collectors.joining(", "));
    }

    public ConnectedUser getConnectedUserByID(UUID id) {
        return connectedUsers.stream()
                .filter(user -> user.id().equals(id))
                .findFirst()
                .orElse(null);
    }

}
