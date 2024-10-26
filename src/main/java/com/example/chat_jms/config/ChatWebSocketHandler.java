package com.example.chat_jms.config;

import com.example.chat_jms.domain.ConnectedUser;
import com.example.chat_jms.domain.PrivateChat;
import com.example.chat_jms.infra.UsersRepository;
import com.example.chat_jms.services.MessageService;
import com.example.chat_jms.dto.*;

import com.example.chat_jms.usecases.SendUserDetails;
import com.example.chat_jms.usecases.SendUsersList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.*;
import jakarta.jms.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
public class ChatWebSocketHandler extends TextWebSocketHandler {

    final private UsersRepository usersRepository;
    final private Connection connection;
    final private MessageService messageService;
    final private Topic globalTopic;
    private Session JMSSession;
    private final SendUserDetails sendUserDetails;

    private final SendUsersList sendUsersList;

    @Autowired
    public ChatWebSocketHandler(UsersRepository usersRepository, Connection connection, MessageService messageService, Topic globalTopic, SendUserDetails sendUserDetails, SendUsersList sendUsersList) {
        this.usersRepository = usersRepository;
        this.connection = connection;
        this.messageService = messageService;
        this.globalTopic = globalTopic;
        this.sendUserDetails = sendUserDetails;
        this.sendUsersList = sendUsersList;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession websocketSession) {
        try {
            String username = getUsername(websocketSession);
            if (username == null || username.trim().isEmpty()) {
                websocketSession.close();
                throw new IllegalArgumentException("Username não pode ser vazio");
            }

            connection.start();

            // Inicia a sessão com JMS
            JMSSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            UUID userID = UUID.randomUUID();

            // Cria a fila do usuario
            Queue queue = JMSSession.createQueue(userID.toString());

            ConnectedUser connectedUser = new ConnectedUser(
                    userID,
                    username,
                    queue,
                    websocketSession);

            // Salva o usuario no banco de dados
            usersRepository.add(connectedUser);

            // Seta o usuario como consumidor do topico
            MessageConsumer topicConsumer = JMSSession.createConsumer(globalTopic);
            MessageConsumer privateConsumer = JMSSession.createConsumer(queue);
            messageService.setConsumerListener(topicConsumer, websocketSession);
            messageService.setConsumerListener(privateConsumer, websocketSession);

            // Envia para o usuario seu ID e Username para sincronização
            sendUserDetails.execute(websocketSession, connectedUser);

            // Notifica todos os usuarios sobre a nova conexão
            sendUsersList.execute();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
        try {

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadNode = objectMapper.readTree(message.getPayload());

            // Verifica qual o tipo de mensagem recebida
            String type = payloadNode.get("type").asText();

            // Verifica se a mensagem é do tipo TopicDTO ou queue
            if ("topic".equalsIgnoreCase(type)) {

                // Mapeia para TopicDTO
                TopicDTO topicMessage = objectMapper.treeToValue(payloadNode, TopicDTO.class);

                // Envia a mensagem para o topico global
                messageService.sendTopicMessage(JMSSession, globalTopic, topicMessage.toString());
            } else if ("queue".equalsIgnoreCase(type)) {
                // Mapeia para QueueDTO
                QueueDTO queueMessage = objectMapper.treeToValue(payloadNode, QueueDTO.class);

                // Recupera o remetente e destinatário
                ConnectedUser sender = usersRepository.getConnectedUserByID(queueMessage.sender());
                ConnectedUser receiver = usersRepository.getConnectedUserByID(queueMessage.receiver());

                String combinedID = (sender.id().compareTo(receiver.id()) < 0)
                        ? sender.id() + "-" + receiver.id()
                        : receiver.id() + "-" + sender.id();

                // Verifica se existe um chat aberto com esses dois usuarios
                PrivateChat privateChat = usersRepository.getPrivateChatById(combinedID);

                if (privateChat != null) {
                    for (ConnectedUser participant : privateChat.participants()) {
                        messageService.sendPrivateMessage(JMSSession, participant.queue(), queueMessage.toString());
                    }

                } else {
                    // Adiciona o chat Privado a lista
                    HashSet<ConnectedUser> participants = new HashSet<>();
                    participants.add(sender);
                    participants.add(receiver);
                    usersRepository.updateOpenChats(new PrivateChat(combinedID, participants));
                    // Envia a mensagem
                    for (ConnectedUser participant : participants) {
                        messageService.sendPrivateMessage(JMSSession, participant.queue(), queueMessage.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        try {
            usersRepository.removeBySession(session);
            sendUsersList.execute();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    private String getUsername(WebSocketSession session) {
        URI uri = session.getUri();
        String query = uri != null ? uri.getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && "username".equals(pair[0])) {
                    return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);

                }
            }
        }
        return null;
    }

}
