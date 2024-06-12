package org.example.api.utilities;

import java.util.LinkedList;
import java.util.Queue;

public class ServerResponsesQueue<T> {

    private final Queue<T> queue;


    public ServerResponsesQueue() {
        queue = new LinkedList<>();
    }


    public void add(T receivedAnnouncement) {
        synchronized (queue) {
            queue.add(receivedAnnouncement);
        }
    }


    public T poll() {
        synchronized (queue) {
            return queue.poll();
        }
    }


    public T peek() {
        synchronized (queue) {
            return queue.peek();
        }
    }

}
