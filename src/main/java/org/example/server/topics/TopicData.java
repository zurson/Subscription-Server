package org.example.server.topics;

import lombok.Data;
import org.example.client.ClientThread;

import java.util.HashSet;
import java.util.Set;

@Data
public class TopicData {
    private final ClientThread producer;
    private final Set<ClientThread> subscribers;

    public TopicData(ClientThread producer) {
        this.producer = producer;
        this.subscribers = new HashSet<>();
    }

    public TopicData(ClientThread producer, Set<ClientThread> subscribers) {
        this.producer = producer;
        this.subscribers = subscribers;
    }

}
