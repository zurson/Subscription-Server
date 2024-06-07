package org.example.server;

import java.net.Socket;
import java.util.Set;

public record TopicData(Socket producer, String producerId, Set<Socket> subscribers) {

}
