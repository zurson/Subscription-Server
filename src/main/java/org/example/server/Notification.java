package org.example.server;

import org.example.client.ClientThread;

import java.util.List;

public record Notification(String content, List<ClientThread> recipients) {
    @Override
    public String toString() {
        return "Notification{" +
                "content='" + content + '\'' +
                ", recipients=" + recipients +
                '}';
    }
}
