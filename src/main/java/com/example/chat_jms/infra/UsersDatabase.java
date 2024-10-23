package com.example.chat_jms.infra;

import jakarta.jms.Queue;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Repository
public class UsersDatabase {

    private final Map<Users, Queue> users;

    public UsersDatabase(Map<Users, Queue> users) {
        this.users = users;
    }

    public void add(Users user, Queue queue) {
        this.users.put(user, queue);
    }

    public void remove(Users user) {
        this.users.remove(user);
    }

    public Queue getQueueByUser(Users user) {

        System.out.println(users.toString());

        System.out.println(users.get(user).toString());

        return users.get(user);
    }

    public String getConnectedUsersAsString() {
        return users.keySet().stream()
                .map(user -> "(" + user.id() + ", " + user.name() + ")")
                .collect(Collectors.joining(", "));
    }

    // Método que retorna a lista de usuários conectados
    public List<Users> getConnectedUsers() {
        return new ArrayList<>(users.keySet());
    }

}
