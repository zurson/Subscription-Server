package org.example;

import org.example.api.Client;
import org.example.api.utilities.FileHandler;
import org.example.api.utilities.Message;
import org.example.api.utilities.payload.MessagePayload;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        String topicName = "ride a bike";
        String otherTopicName = "eat lunch";


        try {
            Client client = new Client();
            client.start("127.0.0.1", 7, "BIKER");
            client.getServerLogs((success, message) -> {
                if (!success)
                    System.out.println("\nError: " + message);
                else
                    System.out.println("\nSuccess: " + message);
            });

            client.createProducer(topicName);

//            client.createSubscriber(topicName, message -> {
//                try {
//                    handleSubscriptionMessage(topicName, message);
//                } catch (IOException e) {
//                    System.err.println(e.getMessage());
//                }
//            });


            System.out.println("Client STATUS: " + client.getStatus());

            client.getServerStatus(statuses -> {
                System.out.println("\nServer STATUS: " + statuses);
            });


//            int counter = 0;
//            while (counter++ < 3) {
//                client.produce(topicName, new MessagePayload("Bike is waiting!"));
//                client.sendFile(topicName, "example.txt");
//                Thread.sleep(3000);
//            }
//
//            client.withdrawProducer(topicName);


//            client.createSubscriber(topicName + "y", message -> {
//                MessagePayload messagePayload = (MessagePayload) message.getPayload();
//                System.out.println("\nPowiadomienie z: " + message.getTopic());
//                System.out.println(messagePayload.getMessage() + "\n");
//            });

//            client.produce(topicName, new MessagePayload("produce for normal"));
//            client.produce(topicName + "y", new MessagePayload("produce for y"));



//            client.stop();

//            client.withdrawSubscriber(topicName);
//            client.produce(topicName, new MessagePayload("produce for normal"));
//            client.produce(topicName, new MessagePayload("produce for normal"));
//            client.produce(topicName, new MessagePayload("produce for normal"));

//            Thread.sleep(5000);
//            client.produce(topicName, new MessagePayload("produce for normal"));

//            client.createSubscriber(topicName + "2222", message -> {
//                System.out.println("YEAH");
//            });

//            client.withdrawProducer(topicName);

//            client.getServerStatus(message -> {
//                System.out.println("\nSTATUS:");
//                System.out.println(message);
//            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static void handleSubscriptionMessage(String topicName, Message message) throws IOException {
        MessagePayload payload = (MessagePayload) message.getPayload();
        String payloadMessage = payload.getMessage();

        if (message.getType().equals("file")) {
            String filename = payloadMessage.substring(0, payloadMessage.indexOf("|"));
            String fileContent = payloadMessage.substring(payloadMessage.indexOf("|") + 1);

            new FileHandler(filename).writeFile(fileContent);

            System.out.println("\nNotification from: " + message.getTopic() + " (" + message.getType() + ")");
            System.out.println(filename + "\n" + fileContent);

            return;
        }

        System.out.println("\nNotification from: " + topicName + " (" + message.getType() + ")");
        System.out.println(payloadMessage);
    }
}