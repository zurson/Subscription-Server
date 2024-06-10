package org.example.server.receive_message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.client.ClientThread;
import org.example.interfaces.MessagesQueueDriver;
import org.example.interfaces.TopicsDriver;
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

    public ReceivedMessagesQueueMonitorThread(MessagesQueueDriver messagesQueueDriver, TopicsDriver topicsDriver) {
        this.messagesQueueDriver = messagesQueueDriver;
        this.topicsDriver = topicsDriver;

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

        Optional<String> errorMessage;

        switch (message.getType()) {
            case "register":
                errorMessage = register(client, message);
                break;

            default:
                errorMessage = Optional.of("Unexpected error");
        }

        if (errorMessage.isEmpty()) {
            client.setClientId(message.getSenderId());
            errorMessage = Optional.of("SUCCESS");
        }

        messagesQueueDriver.addMessageToSendQueue(errorMessage.get(), Collections.singletonList(client));
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

    private Optional<String> register(ClientThread producer, Message message) {
        RegisterPayload payload = (RegisterPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, RegisterPayload.class);
        if (errorMessage.isPresent())
            return errorMessage;

        return switch (message.getMode()) {
            case "producer" -> registerTopic(producer, message);
            case "subscriber" -> addSubscription(producer, message);
            default -> Optional.of("Unexpected error");
        };


    }


    private Optional<String> registerTopic(ClientThread producer, Message message) {
        if (topicsDriver.topicExists(message.getTopic())) return Optional.of("Topic already exists");

        if (topicsDriver.producerExists(message.getSenderId()))
            return Optional.of("Producer with given ID already exists");

        topicsDriver.addTopic(message.getTopic(), new TopicData(producer));
        return Optional.empty();
    }


    private Optional<String> addSubscription(ClientThread subscriber, Message message) {
        if (!topicsDriver.topicExists(message.getTopic())) return Optional.of("Topic does not exists");

        topicsDriver.addSubscriber(message.getTopic(), subscriber);
        return Optional.empty();
    }

}
