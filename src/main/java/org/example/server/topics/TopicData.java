package org.example.server.topics;

import org.example.client.ClientThread;

import java.net.Socket;
import java.util.Set;

public record TopicData(ClientThread producer, Set<ClientThread> subscribers) {

}
