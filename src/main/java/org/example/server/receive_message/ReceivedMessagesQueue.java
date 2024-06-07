package org.example.server.receive_message;

import java.util.LinkedList;
import java.util.Queue;

public class ReceivedMessagesQueue<T> {

    private final Queue<T> receivedMessagesQueue;


    public ReceivedMessagesQueue() {
        receivedMessagesQueue = new LinkedList<>();
    }


    public void add(T receivedAnnouncement) {
        synchronized (receivedMessagesQueue) {
            receivedMessagesQueue.add(receivedAnnouncement);
        }
    }


    public T poll() {
        synchronized (receivedMessagesQueue) {
            return receivedMessagesQueue.poll();
        }
    }

}
