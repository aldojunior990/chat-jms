package com.example.chat_jms.config;

import com.example.chat_jms.domain.Message;
import com.example.chat_jms.infra.UsersDatabase;
import com.example.chat_jms.services.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final UsersDatabase usersDatabase;
    private final Connection connection;
    private final MessageService messageService;
    private final Topic topic;
    private Session JMSSession;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Construtor com injeção de dependência
    @Autowired
    public ChatWebSocketHandler(UsersDatabase usersDatabase, Connection connection,
                                MessageService messageService, Topic topic) {
        this.usersDatabase = usersDatabase;
        this.connection = connection;
        this.messageService = messageService;
        this.topic = topic;
    }

//    @Autowired
//    private UsersDatabase usersDatabase;

//    @Autowired
//    private Connection connection;

//    @Autowired
//    private MessageService messageService;

//    @Autowired
//    private Topic topic;



    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession WSSession) {
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
            //WSSession.sendMessage(new TextMessage("Usuarios ativos: " + usersDatabase.getConnectedUsersAsString()));
            // Cria um objeto com a lista de usuários ativos

            List<String> activeUsers = usersDatabase.getConnectedUsers(); // Supondo que isso retorne uma lista de nomes
            // Cria um objeto JSON
            Map<String, Object> response = new HashMap<>();
            response.put("type", "activeUsers");
            response.put("users", activeUsers);

            // Serializa o objeto para JSON
            String jsonResponse = objectMapper.writeValueAsString(response);

            // Envia a lista de usuários ativos como JSON
            WSSession.sendMessage(new TextMessage(jsonResponse));
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
}
