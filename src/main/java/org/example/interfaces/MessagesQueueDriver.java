package org.example.interfaces;

import org.example.client.ClientThread;
import org.example.server.receive_message.ReceivedMessage;

import java.util.List;

public interface MessagesQueueDriver {

    ReceivedMessage pollMessage();

    void addMessageToSendQueue(String content, List<ClientThread> recipients);

}
