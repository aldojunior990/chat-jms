package com.example.chat_jms.config;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;

@Configuration
@EnableJms
public class ActiveMQConfig {

    @Value("${spring.activemq.broker-url}")
    private String url;

    @Value("${spring.activemq.user}")
    private String user;

    @Value("${spring.activemq.password}")
    private String password;

    @Bean
    public Connection connectionFactory() {
        try {
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
            return connectionFactory.createConnection();
        } catch (Exception err) {
            err.printStackTrace();
            return null;
        }
    }

    @Bean
    public Topic getTopic() {
        return new ActiveMQTopic("global");
    }
}
