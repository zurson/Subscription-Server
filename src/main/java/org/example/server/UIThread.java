package org.example.server;

import org.example.client.ClientThread;
import org.example.config.Config;
import org.example.interfaces.ServerController;
import org.example.server.topics.TopicData;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class UIThread extends Thread {

    private final ServerController serverController;
    private final AtomicBoolean running;
    private final BufferedReader bufferedReader;

    public UIThread(ServerController serverController) {
        this.serverController = serverController;
        this.running = new AtomicBoolean(false);
        bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            try {
                Thread.sleep(1);
                System.out.print("\nEnter command: ");

                while (!bufferedReader.ready())
                    Thread.sleep(1);

                String input = bufferedReader.readLine().trim();

                if (input.isBlank())
                    continue;

                boolean status = processCommand(input);

                if (!status)
                    System.out.println("Command not found!");

            } catch (InterruptedException | IOException e){
                System.err.println("Closing UIThread (" + e.getMessage() + ")");
                break;
            }
        }

    }


    public synchronized void stopThread() {
        if (!running.get())
            return;

        running.set(false);

        try {
            bufferedReader.close();
            this.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean processCommand(String command) {
        return switch (command) {
            case "info" -> {
                showSeverInfo();
                yield true;
            }

            case "connections" -> {
                showConnections();
                yield true;
            }

            case "server" -> {
                showServerDetails();
                yield true;
            }

            case "stop" -> {
                stopServer();
                yield true;
            }

            default -> false;
        };
    }


    /* INFO */


    private void showSeverInfo() {
        Map<String, TopicData> topics = serverController.getTopics();

        if (topics.isEmpty()) {
            System.out.println("No topics found!");
            return;
        }

        String message = getServerInfoMessage(topics);
        System.out.println(message);
    }


    private String getServerInfoMessage(Map<String, TopicData> topics) {
        StringBuilder sb = new StringBuilder();
        String headerAndFooter = "---------- [REGISTERED TOPICS] ----------";
        sb.append(headerAndFooter).append("\n");

        for (Map.Entry<String, TopicData> entry : topics.entrySet()) {
            sb.append("\nTopic: ").append(entry.getKey()).append("\n");
            sb.append("ProducerId: ").append(entry.getValue().getProducer().getClientId()).append("\n");
            sb.append("Subscribers: ").append("\n");
            addSubscribers(sb, entry.getValue().getSubscribers());
        }

        sb.append(headerAndFooter);
        return sb.toString();
    }


    private void addSubscribers(StringBuilder sb, Set<ClientThread> subscribers) {
        for (ClientThread subscriber : subscribers)
            sb.append("\t- ").append(subscriber.getClientId()).append("\n");
    }


    /* CONNECTIONS */


    private void showConnections() {
        StringBuilder sb = new StringBuilder();
        List<ClientThread> connectedClients = serverController.getConnectedClients();

        String headerAndFooter = "---------- [CONNECTIONS] ----------";
        sb.append(headerAndFooter).append("\n");
        sb.append(connectedClients).append("\n");
        sb.append(headerAndFooter);

        System.out.println(sb);
    }


    /* DETAILS */


    private void showServerDetails() {
        StringBuilder sb = new StringBuilder();
        Config serverConfig = serverController.getServerConfig();

        String headerAndFooter = "---------- [SERVER DETAILS] ----------";
        sb.append(headerAndFooter).append("\n");

        sb.append("Port: ").append(serverConfig.getListenPort()).append("\n");
        sb.append("ListenAddresses: ").append(serverConfig.getListenAddresses()).append("\n");

        sb.append(headerAndFooter);

        System.out.println(sb);
    }


    /* STOP */


    private void stopServer() {
        serverController.stopServer();
    }

}
