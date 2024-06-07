package org.example.server;

import org.example.client.ClientThread;
import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.exceptions.ValidationException;
import org.example.interfaces.*;
import org.example.utilities.Validator;

import java.io.IOException;
import java.util.*;

public class Server implements Runnable, ClientsListDriver, ReceiveDriver, MessagesQueueDriver, TopicsDriver {
    private final Config config;
    private final Thread communicationThread, uiThread, receivedMessagesQueueMonitorThread;

    private final Set<ClientThread> clientList;

    private final Map<String, TopicData> registeredTopics;                         // LT
    private final MessagesToSendQueue messagesToSendQueue;                         // KKW
    private final ReceivedMessagesQueue<ReceivedMessage> receivedMessagesQueue;    // KKO


    public Server(String hostname, int port, int timeout) throws IOException, ValidationException {
        if (!Validator.isValidPort(port))
            throw new ValidationException("Invalid port");

        if (!Validator.isValidIPv4Address(hostname))
            throw new ValidationException("Invalid hostname");

        if (!Validator.isValidTimeout(timeout))
            throw new ValidationException("Invalid timeout value");

        this.config = new ConfigLoader().loadConfig();

        this.communicationThread = new CommunicationThread(this, this, hostname, port, timeout);
        this.receivedMessagesQueueMonitorThread = new ReceivedMessagesQueueMonitorThread(this);
        this.uiThread = new UIThread();

        this.clientList = Collections.synchronizedSet(new HashSet<>());
        this.registeredTopics = Collections.synchronizedMap(new HashMap<>());
        this.messagesToSendQueue = new MessagesToSendQueue(getAddElementCallback());
        this.receivedMessagesQueue = new ReceivedMessagesQueue<>();
    }


    private void startServerThreads() {
        uiThread.start();
        communicationThread.start();
        receivedMessagesQueueMonitorThread.start();
    }


    public void stopServer() {
        uiThread.interrupt();
        communicationThread.interrupt();
    }


    private AddElementCallback getAddElementCallback() {
        return () -> {
            Notification notification = messagesToSendQueue.poll();

            for (ClientThread clientThread : notification.recipients()) {
                try {
                    clientThread.sendMessage(notification.content());
                } catch (IOException e) {
                    e.printStackTrace();
                    clientThread.disconnect();
                }
            }
        };
    }


    @Override
    public void run() {
        startServerThreads();
    }


    @Override
    public void addClient(ClientThread clientThread) {
        clientList.add(clientThread);
        System.out.println(clientList);
    }


    @Override
    public void removeClient(ClientThread clientThread) {
        clientList.remove(clientThread);
        System.out.println(clientList);
    }


    @Override
    public void addNewMessage(ReceivedMessage receivedMessage) {
        receivedMessagesQueue.add(receivedMessage);
    }


    @Override
    public ReceivedMessage pollMessage() {
        return receivedMessagesQueue.poll();
    }


    @Override
    public void addMessageToSendQueue(String content, List<ClientThread> recipients) {
        Notification notification = new Notification(content, recipients);
        messagesToSendQueue.add(notification);
    }


    @Override
    public void addTopic(String topicName, TopicData topicData) {
        registeredTopics.put(topicName, topicData);
    }


    @Override
    public void removeTopic(String topicName) {
        registeredTopics.remove(topicName);
    }
}
