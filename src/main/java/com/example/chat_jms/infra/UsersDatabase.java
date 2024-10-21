package com.example.chat_jms.infra;

import jakarta.jms.Queue;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Repository
public class UsersDatabase {

    private final Map<String, Queue> users;

    public UsersDatabase(Map<String, Queue> users) {
        this.users = users;
    }

    public void add(String user, Queue queue) {
        this.users.put(user, queue);
    }

    public void remove(String user) {
        this.users.remove(user);
    }

    public Queue getQueueByUser(String user) {

        System.out.println(users.toString());

        System.out.println(users.get(user).toString());

        return users.get(user);
    }

    // Novo metodo que retorna a lista de usuários conectados
    public List<String> getConnectedUsers() {
        return new ArrayList<>(users.keySet());
    }

//    public String getConnectedUsersAsString() {
//        return String.join(", ", users.keySet());
//    }

}