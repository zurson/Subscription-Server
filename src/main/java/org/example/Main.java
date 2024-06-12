package org.example;

import org.example.api.Client;
import org.example.api.utilities.payload.FeedbackPayload;
import org.example.api.utilities.payload.MessagePayload;

public class Main {
    public static void main(String[] args) {
        String topicName = "test topic";


        try {
            Client client = new Client();
            client.start("127.0.0.1", 7, "CID");

            client.createProducer(topicName);
//            client.createProducer(topicName + "x");
//            client.createProducer(topicName + "y");

            client.createSubscriber(topicName, message -> {
                FeedbackPayload feedbackPayload = (FeedbackPayload) message.getPayload();
                System.out.println("\nPowiadomienie z: " + message.getTopic());
                System.out.println(feedbackPayload.getMessage());
            });

            String mess = client.getStatus();
            System.out.println("STATUS: " + mess);

            client.produce(topicName, new MessagePayload("test message lol"));



//            client.getServerStatus(message -> {
//                System.out.println("\nSTATUS:");
//                System.out.println(message);
//            });

//            client.stop();

//            client.withdrawSubscriber(topicName);

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