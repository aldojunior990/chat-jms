package com.example.chat_jms.config;

import com.example.chat_jms.domain.Message;
import com.example.chat_jms.infra.UsersDatabase;
import com.example.chat_jms.services.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class ChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private UsersDatabase usersDatabase;

    @Autowired
    private Connection connection;

    @Autowired
    private MessageService messageService;

    @Autowired
    private Topic topic;

    private Session JMSSession;


    @Override
    public void afterConnectionEstablished(WebSocketSession WSSession) {
        try {
            String username = getUsername(WSSession);

            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username não pode ser vazio");
            }

            connection.start();

            // Inicia a conexãp
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
            usersDatabase.add(username, queue);

            // Envia a lista de usuarios ativos
            respondWithActiveUsers(WSSession);

        } catch (Exception err) {
            err.printStackTrace();
        }

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {

            // Mapeia a resposta para um padrão
            ObjectMapper objectMapper = new ObjectMapper();
            Message payload = objectMapper.readValue(message.getPayload(), Message.class);

            // Verifica se a mensagem é do tipo topic ou queue
            if ("topic".equalsIgnoreCase(payload.type())) {
                // Envia para o topico
                messageService.sendTopicMessage(JMSSession, topic, message.getPayload());


            } else if ("queue".equalsIgnoreCase(payload.type())) {

                // Recupera o nome do destinatário
                String receiverName = payload.receiver();

                // Recupera a queue do destinatário
                Queue receiverQueue = usersDatabase.getQueueByUser(receiverName);

                // Envia para a queue
                messageService.sendPrivateMessage(JMSSession, receiverQueue, message.getPayload());

            } else {
                session.sendMessage(new TextMessage("Tipo de mensagem inválido."));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        try {
            usersDatabase.remove(getUsername(session));
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
        String activeUsers = usersDatabase.getConnectedUsersAsString(getUsername(session));

        Message message = new Message(
                "users",
                "topic," + activeUsers,
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
