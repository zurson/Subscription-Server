package org.example.server;

import org.example.client.ClientThread;

public record ReceivedMessage(String content, ClientThread client) {
    @Override
    public String toString() {
        return "ReceivedMessage{" +
                "content='" + content + '\'' +
                ", client=" + client +
                '}';
    }
}
