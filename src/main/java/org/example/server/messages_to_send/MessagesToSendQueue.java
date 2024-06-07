package org.example.server.messages_to_send;

import org.example.interfaces.AddElementCallback;

import java.util.LinkedList;
import java.util.Queue;

public class MessagesToSendQueue {

    private final Queue<Notification> messagesToClientsQueue;
    private final AddElementCallback callback;


    public MessagesToSendQueue(AddElementCallback callback) {
        this.messagesToClientsQueue = new LinkedList<>();
        this.callback = callback;
    }


    public void add(Notification notification) {
        synchronized (messagesToClientsQueue) {
            messagesToClientsQueue.add(notification);
            callback.onElementAdded();
        }
    }


    public Notification poll() {
        synchronized (messagesToClientsQueue) {
            return messagesToClientsQueue.poll();
        }
    }

}
