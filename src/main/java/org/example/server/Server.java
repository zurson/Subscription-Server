package org.example.server;

import org.example.client.ClientThread;
import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.exceptions.ValidationException;
import org.example.interfaces.AddElementCallback;
import org.example.interfaces.ServerDriver;
import org.example.utilities.Validator;

import java.io.IOException;
import java.util.*;

public class Server implements Runnable, ServerDriver {
    private final int port;
    private final int timeout;
    private final String hostname;
    private final Config config;
    private final Thread communicationThread;
    private final Thread uiThread;

    private final Map<String, TopicData> topicMap;
    private final Set<ClientThread> clientList;
    private final AddElementCallback addClientCallback;


    public Server(String hostname, int port, int timeout) throws IOException, ValidationException {
        if (!Validator.isValidPort(port))
            throw new ValidationException("Invalid port");

        if (!Validator.isValidIPv4Address(hostname))
            throw new ValidationException("Invalid hostname");

        if (!Validator.isValidTimeout(timeout))
            throw new ValidationException("Invalid timeout value");

        this.hostname = hostname;
        this.port = port;
        this.timeout = timeout;

        this.topicMap = Collections.synchronizedMap(new HashMap<>());
        this.clientList = Collections.synchronizedSet(new HashSet<>());
        addClientCallback = (clientThread) -> System.out.println(clientList);

        this.config = new ConfigLoader().loadConfig();

        this.communicationThread = new CommunicationThread(this, hostname, port, timeout);
        this.uiThread = new UIThread();
    }


    private void startServerThreads() {
        uiThread.start();
        communicationThread.start();
    }


    public void stopServer() {
        uiThread.interrupt();
        communicationThread.interrupt();
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
}
