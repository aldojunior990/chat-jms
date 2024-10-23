package com.example.chat_jms.config;

import com.example.chat_jms.domain.Message;
import com.example.chat_jms.infra.Users;
import com.example.chat_jms.infra.UsersDatabase;
import com.example.chat_jms.services.MessageService;
import com.example.chat_jms.dto.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.*;
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
import java.util.List;
import java.util.UUID;
@Configuration
public class ChatWebSocketHandler extends TextWebSocketHandler {

    final private UsersDatabase usersDatabase;
    final private Connection connection;
    final private MessageService messageService;
    final private Topic topic;
    private Session JMSSession;

    private Users user = null;

    @Autowired
    public ChatWebSocketHandler(UsersDatabase usersDatabase, Connection connection, MessageService messageService, Topic topic) {
        this.usersDatabase = usersDatabase;
        this.connection = connection;
        this.messageService = messageService;
        this.topic = topic;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession WSSession) {
        try {
            String username = getUsername(WSSession);

            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username não pode ser vazio");
            }

            user = new Users(UUID.randomUUID(), username); // cria um usuário

            connection.start();

            // Inicia a conexão
            JMSSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Cria uma fila para o usuario
            Queue queue = JMSSession.createQueue(username);

            // Cria os consumidores do tópico global e individual
            MessageConsumer privateConsumer = JMSSession.createConsumer(queue);
            MessageConsumer topicConsumer = JMSSession.createConsumer(topic);

            // Seta o usuario como consumidor do topico e da queue individual
            messageService.setConsumerListener(privateConsumer, WSSession);
            messageService.setConsumerListener(topicConsumer, WSSession);

            // Salva o usuario no banco de dados com nome e queue
            usersDatabase.add(user, queue);

            respondWithActiveUsers(WSSession);



        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
        try {

            // Mapeia a resposta para um padrão
            // ObjectMapper objectMapper = new ObjectMapper();
            // Message payload = objectMapper.readValue(message.getPayload(), Message.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadNode = objectMapper.readTree(message.getPayload());
            // Verifica qual o tipo de DTO
            String type = payloadNode.get("type").asText();


            // Verifica se a mensagem é do tipo TopicDTO ou queue
            if ("topic".equalsIgnoreCase(type)) {

                // Mapeia para TopicDTO
                TopicDTO topicMessage = objectMapper.treeToValue(payloadNode, TopicDTO.class);

                // Envia para o topico
                messageService.sendTopicMessage(JMSSession, topic, topicMessage.content());

            } else if ("queue".equalsIgnoreCase(type)) {

                // Recupera o nome do destinatário
                // String receiverName = payload.receiver();

                // Recupera a queue do destinatário
                // Queue receiverQueue = usersDatabase.getQueueByUser(user);

                // Envia para a queue
                // messageService.sendPrivateMessage(JMSSession, receiverQueue, message.getPayload());

                // Mapeia para QueueDTO
                QueueDTO queueMessage = objectMapper.treeToValue(payloadNode, QueueDTO.class);

                // Recupera o ID do destinatário
                UUID receiverId = queueMessage.receiverId();

                // Recupera o usuário destinatário baseado no receiverId
                Users receiver = usersDatabase.getConnectedUsers().stream()
                        .filter(user -> user.id().equals(receiverId))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Destinatário não encontrado"));

                // Recupera a fila do destinatário
                Queue receiverQueue = usersDatabase.getQueueByUser(receiver);

                // Envia a mensagem privada para a queue
                messageService.sendPrivateMessage(JMSSession, receiverQueue, queueMessage.content());

            } else if ("users".equalsIgnoreCase(type)) {

                // Pega a lista de usuários conectados
                List<Users> connectedUsers = usersDatabase.getConnectedUsers();

                // Converte a lista de Users para UsersDTO
                List<UsersDTO> usersDTOList = connectedUsers.stream()
                        .map(user -> new UsersDTO("users", user.id(), user.name()))
                        .toList();

                // Converte a lista de UserResponseDTO para JSON
                String usersJson = objectMapper.writeValueAsString(usersDTOList);

                // Envia a lista de UserResponseDTO para o cliente
                session.sendMessage(new TextMessage(usersJson));


            } else {
                session.sendMessage(new TextMessage("Tipo de mensagem inválido."));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        try {
            usersDatabase.remove(user);
            JMSSession.close();
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

    public void respondWithActiveUsers(WebSocketSession session) {
        String activeUsers = usersDatabase.getConnectedUsersAsString();

        Message message = new Message(
                "users",
                activeUsers,
                "server",
                getUsername(session)
        );

        try {
            session.sendMessage(new TextMessage(message.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
