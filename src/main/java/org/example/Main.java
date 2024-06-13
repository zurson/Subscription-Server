package org.example;

import org.example.api.Client;
import org.example.api.utilities.payload.FeedbackPayload;
import org.example.api.utilities.payload.MessagePayload;

public class Main {
    public static void main(String[] args) {
        String topicName = "ride a bike";
        String otherTopicName = "eat lunch";


        try {
            Client client = new Client();
            client.start("127.0.0.1", 7, "BIKER");
            client.getServerLogs((success, message) -> {
                if (!success)
                    System.out.println("Error: " + message);
                else
                    System.out.println("Success: " + message);
            });

            client.createProducer(topicName);

            client.createSubscriber(otherTopicName, message -> {
                MessagePayload messagePayload = (MessagePayload) message.getPayload();
                System.out.println("\nPowiadomienie z: " + message.getTopic());
                System.out.println(messagePayload.getMessage() + "\n");
            });

            int counter = 0;
            while(counter++ < 10) {
                client.produce(topicName, new MessagePayload("Bike is waiting!"));
                Thread.sleep(1_000);
            }

            client.withdrawProducer(topicName);







//            client.createSubscriber(topicName + "y", message -> {
//                MessagePayload messagePayload = (MessagePayload) message.getPayload();
//                System.out.println("\nPowiadomienie z: " + message.getTopic());
//                System.out.println(messagePayload.getMessage() + "\n");
//            });

//            client.produce(topicName, new MessagePayload("produce for normal"));
//            client.produce(topicName + "y", new MessagePayload("produce for y"));

//            System.out.println("STATUS: " + client.getStatus());

//            client.getServerStatus(message -> {
//                System.out.println("\nSTATUS:");
//                System.out.println(message);
//            });

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
}