package org.example.server;

import org.example.client.ClientThread;
import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.exceptions.ValidationException;
import org.example.interfaces.*;
import org.example.server.messages_to_send.MessagesToSendQueue;
import org.example.server.messages_to_send.Notification;
import org.example.server.receive_message.ReceivedMessage;
import org.example.server.receive_message.ReceivedMessagesQueue;
import org.example.server.receive_message.ReceivedMessagesQueueMonitorThread;
import org.example.server.topics.TopicData;
import org.example.utilities.Validator;

import java.io.IOException;
import java.util.*;

public class Server implements Runnable, ClientsListDriver, ReceiveDriver, MessagesQueueDriver, TopicsDriver, ServerController {
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
        this.receivedMessagesQueueMonitorThread = new ReceivedMessagesQueueMonitorThread(this, this, this);
        this.uiThread = new UIThread(this);

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


    /* ClientsListDriver */


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


    /* ReceiveDriver */


    @Override
    public void addNewMessage(ReceivedMessage receivedMessage) {
        receivedMessagesQueue.add(receivedMessage);
    }


    /* MessagesQueueDriver */


    @Override
    public ReceivedMessage pollMessage() {
        return receivedMessagesQueue.poll();
    }


    @Override
    public void addMessageToSendQueue(String content, List<ClientThread> recipients) {
        Notification notification = new Notification(content, recipients);
        messagesToSendQueue.add(notification);
    }


    /* TopicsDriver */


    @Override
    public void addTopic(String topicName, TopicData topicData) {
        registeredTopics.put(topicName, topicData);

        synchronized (registeredTopics) {
            System.out.println("Added new topic: " + topicName);
            for (Map.Entry<String, TopicData> entry : registeredTopics.entrySet()) {
                System.out.print(entry.getKey() + ": " + entry.getValue().getSubscribers() + "\n");
            }
        }
    }


    @Override
    public void removeTopic(String topicName) {
        registeredTopics.remove(topicName);
    }


    @Override
    public boolean topicExists(String topicName) {
        synchronized (registeredTopics) {
            return registeredTopics.containsKey(topicName);
        }
    }


    @Override
    public boolean producerExists(String producerId) {
        synchronized (registeredTopics) {
            for (TopicData topicData : registeredTopics.values()) {
                if (topicData.getProducer().getClientId().equalsIgnoreCase(producerId))
                    return true;
            }

            return false;
        }
    }


    @Override
    public void addSubscriber(String topicName, ClientThread subscriber) {
        synchronized (registeredTopics) {
            TopicData topicData = getTopic(topicName);
            topicData.getSubscribers().add(subscriber);
            System.out.println("\nAdded new subscriber: " + subscriber + " to: " + topicName);
        }
    }


    @Override
    public TopicData getTopic(String topicName) {
        synchronized (registeredTopics) {
            TopicData topicData = registeredTopics.get(topicName);
            return new TopicData(topicData.getProducer(), topicData.getSubscribers());
        }
    }


    /* ServerController */


    @Override
    public Map<String, TopicData> getTopics() {
        synchronized (registeredTopics) {
            HashMap<String, TopicData> topicsCopy = new HashMap<>();

            for (Map.Entry<String, TopicData> entry : registeredTopics.entrySet()) {
                String title = entry.getKey();
                TopicData topicData = new TopicData(entry.getValue().getProducer(), entry.getValue().getSubscribers());
                topicsCopy.put(title, topicData);
            }

            return topicsCopy;
        }
    }


    @Override
    public Object getTopicSynchronizer() {
        return registeredTopics;
    }

    @Override
    public List<ClientThread> getConnectedClients() {
        return new ArrayList<>(clientList);
    }
}
