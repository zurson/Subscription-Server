package org.example.server;

import lombok.Data;

import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

@Data
public class TopicData {

    private Socket producer;
    private Set<Socket> subscribers;


    public TopicData(Socket producer) {
        this.producer = producer;
        subscribers = new HashSet<>();
    }


}
