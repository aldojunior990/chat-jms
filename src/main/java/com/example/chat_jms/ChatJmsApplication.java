package com.example.chat_jms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJms
public class ChatJmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatJmsApplication.class, args);
    }

}
