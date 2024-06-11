package org.example.server.receive_message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.client.ClientThread;
import org.example.interfaces.MessagesQueueDriver;
import org.example.interfaces.ServerController;
import org.example.interfaces.TopicsDriver;
import org.example.server.receive_message.message.MessagePayload;
import org.example.server.receive_message.message.MessageResponse;
import org.example.server.receive_message.register.RegisterPayload;
import org.example.server.receive_message.status.StatusPayload;
import org.example.server.receive_message.status.StatusResponse;
import org.example.server.receive_message.status.StatusResponseBuilder;
import org.example.server.receive_message.withdraw.SubscriptionRemoveData;
import org.example.server.receive_message.withdraw.WithdrawPayload;
import org.example.server.topics.TopicData;
import org.example.utilities.Validator;

import javax.validation.ConstraintViolation;
import java.util.*;

import static org.example.settings.Settings.QUEUE_CHECK_INTERVAL_MS;

public class ReceivedMessagesQueueMonitorThread extends Thread {

    private final ObjectMapper mapper;
    private final MessagesQueueDriver messagesQueueDriver;
    private final TopicsDriver topicsDriver;
    private final ServerController serverController;

    public ReceivedMessagesQueueMonitorThread(MessagesQueueDriver messagesQueueDriver, TopicsDriver topicsDriver, ServerController serverController) {
        this.messagesQueueDriver = messagesQueueDriver;
        this.topicsDriver = topicsDriver;
        this.serverController = serverController;

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }


    @Override
    public void run() {

        try {
            while (true) {
                ReceivedMessage receivedMessage = messagesQueueDriver.pollMessage();

                if (receivedMessage == null) {
                    Thread.sleep(QUEUE_CHECK_INTERVAL_MS);
                    continue;
                }

                manageMessage(receivedMessage);
            }
        } catch (InterruptedException ignored) {
        }

    }


    private void manageMessage(ReceivedMessage receivedMessage) {
//        synchronized (serverController.getTopicSynchronizer()) {
        String content = receivedMessage.content();
        ClientThread client = receivedMessage.client();

        Message message = mapReceivedMessage(content);

        if (message == null) {
            addMessageToQueue("Validation error", client);
            return;
        }

        if (changingExistingId(client, message)) {
            addMessageToQueue("Cannot to change client id", client);
            return;
        }

        if (!isPermitted(message.getSenderId(), client)) {
            addMessageToQueue("Given ID is busy", client);
            client.disconnect();
            return;
        }

        String responseMessage;
        List<ClientThread> recipients = new ArrayList<>();

        switch (message.getType()) {
            case "register":
                responseMessage = register(client, message);
                recipients.add(client);
                break;

            case "status":
                responseMessage = status(message);
                recipients.add(client);
                break;

            case "message":
                MessageResponse response = message(client, message);
                responseMessage = response.getContent();
                recipients = response.getRecipients();
                break;

            case "withdraw":
                responseMessage = withdraw(client, message);
                recipients.add(client);
                break;

            default:
                responseMessage = "Unexpected error";
                recipients.add(client);
                break;
        }

        if (recipients.isEmpty() || responseMessage == null)
            return;

        client.setClientId(message.getSenderId());
        addMessageToQueue(responseMessage, recipients);
//        }
    }


    private Message mapReceivedMessage(String receivedMessage) {
        if (receivedMessage == null || receivedMessage.isEmpty()) {
            return null;
        }

        try {
            String modifiedJson = insertTypeToPayload(receivedMessage);
            Message message = mapper.readValue(modifiedJson, Message.class);

            Set<ConstraintViolation<Message>> violations = Validator.validateJsonObject(message);
            if (violations == null || !violations.isEmpty())
                return null;

            return message;
        } catch (JsonProcessingException e) {
            return null;
        }

    }


    private String insertTypeToPayload(String receivedMessage) throws JsonProcessingException {
        JsonNode rootNode = mapper.readTree(receivedMessage);
        String type = rootNode.path("type").asText();
        ((ObjectNode) rootNode.path("payload")).put("type", type);

        return mapper.writeValueAsString(rootNode);
    }


    private String removeTypeFromPayload(String receivedMessage) throws JsonProcessingException {
        JsonNode rootNode = mapper.readTree(receivedMessage);
        ((ObjectNode) rootNode.path("payload")).remove("type");
        return mapper.writeValueAsString(rootNode);
    }


    private boolean changingExistingId(ClientThread client, Message message) {
        String currentClientId = client.getClientId();
        String clientIdFromMessage = message.getSenderId();
        return currentClientId != null && !clientIdFromMessage.equals(currentClientId);
    }


    private boolean isTopicProducer(ClientThread client, String topicName) {
        TopicData topicData = topicsDriver.getTopic(topicName);

        if (topicData == null)
            return false;

        return client.equals(topicData.getProducer());
    }


    private boolean isTopicSubscriber(ClientThread client, String topicName) {
        TopicData topicData = topicsDriver.getTopic(topicName);

        if (topicData == null)
            return false;

        return topicData.getSubscribers().contains(client);
    }


    private boolean isPermitted(String clientId, ClientThread client) {
        Map<String, TopicData> topics = serverController.getTopics();

        for (TopicData topicData : topics.values()) {
            ClientThread producer = topicData.getProducer();
            if (producer.getClientId().equals(clientId) && !producer.equals(client))
                return false;
        }

        return true;
    }


    private boolean isSubscriberOrProducer(String topicNameToSkip, ClientThread client) {
        Map<String, TopicData> topics = serverController.getTopics();

        for (Map.Entry<String, TopicData> entry : topics.entrySet()) {
            if (entry.getKey().equals(topicNameToSkip))
                continue;

            TopicData topicData = entry.getValue();
            if (client.equals(topicData.getProducer()) || topicData.getSubscribers().contains(client))
                return true;

        }

        return false;
    }


    private void addMessageToQueue(String message, List<ClientThread> recipients) {
        messagesQueueDriver.addMessageToSendQueue(message, recipients);
    }


    private void addMessageToQueue(String message, ClientThread recipient) {
        addMessageToQueue(message, Collections.singletonList(recipient));
    }


    /* REGISTER */


    private String register(ClientThread producer, Message message) {
        RegisterPayload payload = (RegisterPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, RegisterPayload.class);
        if (errorMessage.isPresent())
            return errorMessage.get();

        return switch (message.getMode()) {
            case "producer" -> registerTopic(producer, message);
            case "subscriber" -> addSubscription(producer, message);
            default -> "Unexpected error";
        };


    }


    private String registerTopic(ClientThread producer, Message message) {
        if (topicsDriver.topicExists(message.getTopic()))
            return "Topic already exists";

        topicsDriver.addTopic(message.getTopic(), new TopicData(producer));
        return "Successfully registered topic: " + message.getTopic();
    }


    private String addSubscription(ClientThread subscriber, Message message) {
        if (!topicsDriver.topicExists(message.getTopic()))
            return "Topic does not exists";

        if (topicsDriver.getTopic(message.getTopic()).getSubscribers().contains(subscriber))
            return "You already subscribes this topic";

        topicsDriver.addSubscriber(message.getTopic(), subscriber);
        return "Successfully subscribed topic: " + message.getTopic();
    }


    /* STATUS */


    private String status(Message message) {
        StatusPayload payload = (StatusPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, StatusPayload.class);
        if (errorMessage.isPresent())
            return errorMessage.get();

        try {
            StatusResponse statusResponse = new StatusResponseBuilder().build(serverController.getTopics());
            return mapper.writeValueAsString(statusResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "Unexpected error";
        }

    }


    /* MESSAGE */


    private MessageResponse message(ClientThread client, Message message) {
        Optional<String> validationError = validateMessage(client, message);

        if (validationError.isPresent())
            return new MessageResponse(validationError.get(), Collections.singletonList(client));

        try {
            TopicData topicData = topicsDriver.getTopic(message.getTopic());
            String content = mapper.writeValueAsString(message);
            content = removeTypeFromPayload(content);

            System.out.println("\nSending: " + content);
            System.out.println("To: " + topicData.getSubscribers());

            List<ClientThread> recipients = new ArrayList<>(topicData.getSubscribers());
            recipients.add(client);

            return new MessageResponse(content, recipients);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new MessageResponse("Unexpected error", Collections.singletonList(client));
        }
    }


    private Optional<String> validateMessage(ClientThread client, Message message) {
        MessagePayload payload = (MessagePayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, MessagePayload.class);
        if (errorMessage.isPresent())
            return errorMessage;

        String topic = message.getTopic();

        if (!topicsDriver.topicExists(topic))
            return Optional.of("Topic does not exists");

        if (!isTopicProducer(client, topic))
            return Optional.of("You are not this topic producer");

        return Optional.empty();
    }


    /* WITHDRAW */


    private String withdraw(ClientThread client, Message message) {
        WithdrawPayload payload = (WithdrawPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, WithdrawPayload.class);
        if (errorMessage.isPresent())
            return errorMessage.get();

        String topicName = message.getTopic();

        if (!topicsDriver.topicExists(topicName))
            return "Topic does not exists";

        if (isTopicProducer(client, topicName)) {
            unregisterTopic(topicName);
            return null;
        }

        if (isTopicSubscriber(client, topicName)) {
            unregisterSubscription(topicName, client);
            return null;
        }

        return "You have nothing in common with given topic";
    }


    private void unregisterTopic(String topicName) {
        TopicData topicData = topicsDriver.getTopic(topicName);

        // Subscribers
        SubscriptionRemoveData subscriptionRemoveData = removeSubscribers(topicData, topicName);
        notifyAboutSubscriptionRemove(subscriptionRemoveData);
        disconnectClients(subscriptionRemoveData.getClientsToDisconnect());

        // Producer
        ClientThread producer = topicData.getProducer();
        addMessageToQueue("Your topic has been unregistered: " + topicName, producer);

        if (!isSubscriberOrProducer(topicName, producer))
            producer.disconnect();

        topicsDriver.removeTopic(topicName);
    }


    private void unregisterSubscription(String topicName, ClientThread client) {
        TopicData topicData = topicsDriver.getTopic(topicName);
        boolean status = topicData.getSubscribers().remove(client);

        if (status)
            addMessageToQueue("Unsubscribed: " + topicName, client);
        else
            addMessageToQueue("Unable to remove subscription", client);

        if (!isSubscriberOrProducer(topicName, client))
            client.disconnect();
    }


    private SubscriptionRemoveData removeSubscribers(TopicData topicData, String topicName) {
        SubscriptionRemoveData removeData = new SubscriptionRemoveData(topicName);

        for (ClientThread subscriber : topicData.getSubscribers()) {
            if (isSubscriberOrProducer(topicName, subscriber)) {
                removeData.addClientToNotify(subscriber);
                continue;
            }

            removeData.addClientToDisconnect(subscriber);
        }

        return removeData;
    }


    private void notifyAboutSubscriptionRemove(SubscriptionRemoveData subscriptionRemoveData) {
        String topicName = subscriptionRemoveData.getTopicName();

        List<ClientThread> recipients = new ArrayList<>();
        recipients.addAll(subscriptionRemoveData.getClientsToNotify());
        recipients.addAll(subscriptionRemoveData.getClientsToDisconnect());

        addMessageToQueue("Topic you subscribed has been removed: " + topicName, recipients);
    }


    private void disconnectClients(List<ClientThread> clients) {
        clients.forEach(ClientThread::disconnect);
    }


}
