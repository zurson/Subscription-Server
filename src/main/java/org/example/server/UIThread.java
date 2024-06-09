package org.example.server;

import org.example.client.ClientThread;
import org.example.interfaces.ServerController;
import org.example.server.topics.TopicData;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class UIThread extends Thread {

    private final Scanner scanner;
    private final ServerController serverController;

    public UIThread(ServerController serverController) {
        this.serverController = serverController;
        scanner = new Scanner(System.in);
    }

    @Override
    public void run() {

        while (true) {
            System.out.print("\nEnter command: ");
            String input = scanner.nextLine().trim();

            if (input.isBlank())
                continue;

            boolean status = processCommand(input);

            if (!status)
                System.out.println("Command not found!");
        }


    }


    private boolean processCommand(String command) {
        switch (command) {
            case "info":
                showSeverInfo();
                return true;

            default:
                return false;
        }
    }


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


}
