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
import org.example.server.receive_message.register.RegisterPayload;
import org.example.server.receive_message.status.StatusPayload;
import org.example.server.receive_message.status.StatusResponse;
import org.example.server.receive_message.status.StatusResponseBuilder;
import org.example.server.topics.TopicData;
import org.example.utilities.Validator;

import javax.validation.ConstraintViolation;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

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
        String content = receivedMessage.content();
        ClientThread client = receivedMessage.client();

        Message message = mapReceivedMessage(content);

        if (message == null) {
            messagesQueueDriver.addMessageToSendQueue("Validation error", Collections.singletonList(client));
            return;
        }

        if (changingExistingId(client, message)) {
            messagesQueueDriver.addMessageToSendQueue("Cannot to change client id", Collections.singletonList(client));
            return;
        }

        String responseMessage;

        switch (message.getType()) {
            case "register":
                responseMessage = register(client, message);
                break;

            case "status":
                responseMessage = status(message);
                break;

            default:
                responseMessage = "Unexpected error";
        }

        client.setClientId(message.getSenderId());
        messagesQueueDriver.addMessageToSendQueue(responseMessage, Collections.singletonList(client));
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


    private boolean changingExistingId(ClientThread client, Message message) {
        String currentClientId = client.getClientId();
        String clientIdFromMessage = message.getSenderId();
        return currentClientId != null && !clientIdFromMessage.equals(currentClientId);
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
}
