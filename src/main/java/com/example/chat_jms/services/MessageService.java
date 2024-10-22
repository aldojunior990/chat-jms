package com.example.chat_jms.services;


import jakarta.jms.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
public class MessageService {

    public void sendPrivateMessage(Session JMSsession, Queue queue, String message) {
        try {
            // Cria o produtor da mensagem
            MessageProducer messageProducer = JMSsession.createProducer(queue);

            TextMessage textMessage = JMSsession.createTextMessage(message);

            // Envia a mensagem
            messageProducer.send(textMessage);

            //Fecha a conexão
            messageProducer.close();
        } catch (JMSException err) {
            err.printStackTrace();
        }
    }

    public void sendTopicMessage(Session JMSsession, Topic topic, String message) {
        try {
            // Cria o produtor
            MessageProducer messageProducer = JMSsession.createProducer(topic);

            TextMessage textMessage = JMSsession.createTextMessage(message);

            // Envia a mensagem
            messageProducer.send(textMessage);

            messageProducer.close();
        } catch (JMSException err) {
            err.printStackTrace();
        }
    }

    public void setConsumerListener(MessageConsumer consumer, WebSocketSession WSSession) {
        try {
            // Seta o usuario como consumidor do topico/fila
            // Se houver alguma nova mensagem ele é notificado
            consumer.setMessageListener(message -> {
                try {
                    if (message instanceof jakarta.jms.TextMessage textMessage) {
                        String text = textMessage.getText();
                        WSSession.sendMessage(new org.springframework.web.socket.TextMessage(text));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception err) {
            err.printStackTrace();
        }

    }
}
