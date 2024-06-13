package org.example.server.receive_message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.client.ClientThread;
import org.example.config.Config;
import org.example.interfaces.MessagesQueueDriver;
import org.example.interfaces.ServerController;
import org.example.interfaces.TopicsDriver;
import org.example.server.receive_message.config.ConfigPayload;
import org.example.server.receive_message.config.ConfigResponse;
import org.example.server.receive_message.message.MessagePayload;
import org.example.server.receive_message.message.MessageResponse;
import org.example.server.receive_message.register.RegisterPayload;
import org.example.server.receive_message.status.StatusPayload;
import org.example.server.receive_message.status.StatusResponseBuilder;
import org.example.server.receive_message.status.StatusResponsePayload;
import org.example.server.receive_message.withdraw.WithdrawPayload;
import org.example.server.topics.TopicData;
import org.example.utilities.Validator;

import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.server.receive_message.FeedbackType.CONFIG;
import static org.example.server.receive_message.FeedbackType.REJECT;
import static org.example.settings.Settings.QUEUE_CHECK_INTERVAL_MS;

public class ReceivedMessagesQueueMonitorThread extends Thread {

    private final ObjectMapper mapper;
    private final MessagesQueueDriver messagesQueueDriver;
    private final TopicsDriver topicsDriver;
    private final ServerController serverController;
    private final AtomicBoolean running;

    public ReceivedMessagesQueueMonitorThread(MessagesQueueDriver messagesQueueDriver, TopicsDriver topicsDriver, ServerController serverController) {
        this.messagesQueueDriver = messagesQueueDriver;
        this.topicsDriver = topicsDriver;
        this.serverController = serverController;
        this.running = new AtomicBoolean(false);

        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }


    @Override
    public void run() {
        running.set(true);

        try {
            while (running.get()) {
                ReceivedMessage receivedMessage = messagesQueueDriver.pollMessage();

                if (receivedMessage == null) {
                    Thread.sleep(QUEUE_CHECK_INTERVAL_MS);
                    continue;
                }

                boolean status = manageMessage(receivedMessage);
                if (status)
                    receivedMessage.client().setActionRequest(true);
            }
        } catch (InterruptedException ignored) {
        }

    }


    public synchronized void stopThread() {
        running.set(false);
    }


    private boolean manageMessage(ReceivedMessage receivedMessage) {
        String content = receivedMessage.content();
        ClientThread client = receivedMessage.client();

        Message message = mapReceivedMessage(content);

        if (message == null) {
            System.err.println("VALIDATION ERROR: " + receivedMessage.content());
            return false;
        }

        if (changingExistingId(client, message)) {
            addErrorMessageToQueue(message, "Cannot to change client id", client);
            return false;
        }

        if (!isPermitted(message.getSenderId(), client)) {
            addErrorMessageToQueue(message, "Given ID is busy", client);
            client.disconnect();
            return false;
        }

        FeedbackPayload payload = null;
        List<ClientThread> recipients = new ArrayList<>();

        switch (message.getType()) {
            case "register":
                payload = register(client, message);
                recipients.add(client);
                break;

            case "status":
                payload = status(message);
                recipients.add(client);
                break;

            case "message":
                MessageResponse response = message(client, message);

                if (!response.isSuccess()) {
                    payload = createFeedbackPayload(false, response.getError());
                    recipients.add(client);
                    break;
                } else {
                    boolean success = !response.getRecipients().isEmpty();
                    payload = createFeedbackPayload(success, "Sent to " + response.getRecipients().size() + " subscribers");
                    recipients.add(client);
                }

                addMessageToQueue(response.getContent(), response.getRecipients());
                break;

            case "withdraw":
                payload = withdraw(client, message);
                recipients.add(client);
                break;

            case "config":
                ConfigResponse configResponse = config(message);
                if (!configResponse.isSuccess()) {
                    payload = createFeedbackPayload(false, "Config unexpected error");
                    recipients.add(client);
                    break;
                }

                payload = createFeedbackPayload(true, configResponse.getConfig());
                payload.setType(CONFIG.getValue());
                recipients.add(client);
                break;

            default:
                payload = createFeedbackPayload(false, "Unexpected error");
                recipients.add(client);
                break;
        }

        if (recipients.isEmpty() || payload == null)
            return false;

        try {
            client.setClientId(message.getSenderId());

            payload.setTimestampOfMessage(message.getTimestamp());
            payload.setTopicOfMessage(message.getTopic());

            message.setType(payload.getType());
            message.setPayload(payload);
            message.setSenderId(serverController.getServerConfig().getServerId());

            String mappedFeedback = mapper.writeValueAsString(message);
            addMessageToQueue(mappedFeedback, recipients);

            return true;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            System.err.println("Map error");
        }

        return false;
    }


    private Message mapReceivedMessage(String receivedMessage) {
        if (receivedMessage == null || receivedMessage.isEmpty())
            return null;

        try {
            String modifiedJson = insertTypeToPayload(receivedMessage);
            Message message = mapper.readValue(modifiedJson, Message.class);

            Set<ConstraintViolation<Message>> violations = Validator.validateJsonObject(message);
            if (violations == null || !violations.isEmpty())
                return null;

            return message;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
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


    private String removeFieldFromJson(String json, String fieldName) throws IOException {
        JsonNode jsonNode = mapper.readTree(json);
        if (jsonNode instanceof ObjectNode) {
            ((ObjectNode) jsonNode).remove(fieldName);
        }
        return mapper.writeValueAsString(jsonNode);
    }


    private boolean changingExistingId(ClientThread client, Message message) {
        String currentClientId = client.getClientId();
        String clientIdFromMessage = message.getSenderId();
        return currentClientId != null && !clientIdFromMessage.equals(currentClientId);
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


    private void addErrorMessageToQueue(Message message, String error, ClientThread recipient) {
        if (message == null)
            return;

        try {
            FeedbackPayload feedbackPayload = createFeedbackPayload(false, error);
            feedbackPayload.setTimestampOfMessage(message.getTimestamp());
            feedbackPayload.setTopicOfMessage(message.getTopic());

            message.setType(feedbackPayload.getType());
            message.setPayload(createFeedbackPayload(false, error));
            message.setSenderId(serverController.getServerConfig().getServerId());

            addMessageToQueue(mapper.writeValueAsString(message), recipient);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    private void addMessageToQueue(String message, List<ClientThread> recipients) {
        messagesQueueDriver.addMessageToSendQueue(message, recipients);
    }


    private void addMessageToQueue(String message, ClientThread recipient) {
        addMessageToQueue(message, Collections.singletonList(recipient));
    }


    private FeedbackPayload createFeedbackPayload(boolean success, String message) {
        FeedbackPayload feedbackPayload = new FeedbackPayload();

        FeedbackType feedbackType = success ? FeedbackType.ACKNOWLEDGE : REJECT;
        feedbackPayload.setType(feedbackType.getValue());

        feedbackPayload.setSuccess(success);
        feedbackPayload.setMessage(message);
        return feedbackPayload;
    }


    /* REGISTER */


    private FeedbackPayload register(ClientThread producer, Message message) {
        RegisterPayload payload = (RegisterPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, RegisterPayload.class);
        if (errorMessage.isPresent())
            return createFeedbackPayload(false, errorMessage.get());


        return switch (message.getMode()) {
            case "producer" -> registerTopic(producer, message);
            case "subscriber" -> addSubscription(producer, message);
            default -> createFeedbackPayload(false, "Unexpected error");
        };
    }


    private FeedbackPayload registerTopic(ClientThread producer, Message message) {
        if (topicsDriver.topicExists(message.getTopic()))
            return createFeedbackPayload(false, "Topic already exists");

        topicsDriver.addTopic(message.getTopic(), new TopicData(producer));
        return createFeedbackPayload(true, "Successfully registered topic: " + message.getTopic());
    }


    private FeedbackPayload addSubscription(ClientThread subscriber, Message message) {
        if (!topicsDriver.topicExists(message.getTopic()))
            return createFeedbackPayload(false, "Topic does not exist");

        if (topicsDriver.getTopic(message.getTopic()).getSubscribers().contains(subscriber))
            return createFeedbackPayload(false, "You already subscribes this topic");

        topicsDriver.addSubscriber(message.getTopic(), subscriber);
        return createFeedbackPayload(true, "Successfully subscribed topic: " + message.getTopic());
    }


    /* STATUS */


    private FeedbackPayload status(Message message) {
        StatusPayload payload = (StatusPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, StatusPayload.class);
        if (errorMessage.isPresent())
            return createFeedbackPayload(false, errorMessage.get());

        StatusResponsePayload statusResponsePayload = new StatusResponseBuilder().build(serverController.getTopics());
        statusResponsePayload.setSuccess(true);

        try {
            String mappedStatusResponsePayload = mapper.writeValueAsString(statusResponsePayload);
            mappedStatusResponsePayload = removeFieldFromJson(mappedStatusResponsePayload, "type");
            return createFeedbackPayload(true, mappedStatusResponsePayload);
        } catch (IOException e) {
            return createFeedbackPayload(false, "Unexpected error");
        }

//        return statusResponsePayload;
    }


    /* MESSAGE */


    private MessageResponse message(ClientThread client, Message message) {
        Optional<String> validationError = validateMessage(client, message);

        if (validationError.isPresent())
            return new MessageResponse(null, Collections.singletonList(client), false, validationError.get());

        try {
            TopicData topicData = topicsDriver.getTopic(message.getTopic());
            String content = mapper.writeValueAsString(message);
            content = removeTypeFromPayload(content);

            List<ClientThread> recipients = new ArrayList<>(topicData.getSubscribers());

            return new MessageResponse(content, recipients, true, null);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new MessageResponse(null, Collections.singletonList(client), false, "Unexpected error");
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

        if (!topicsDriver.isTopicProducer(client, topic))
            return Optional.of("You are not this topic producer");

        return Optional.empty();
    }


    /* WITHDRAW */


    private FeedbackPayload withdraw(ClientThread client, Message message) {
        WithdrawPayload payload = (WithdrawPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, WithdrawPayload.class);
        if (errorMessage.isPresent())
            return createFeedbackPayload(false, errorMessage.get());

        String topicName = message.getTopic();

        if (!topicsDriver.topicExists(topicName))
            return createFeedbackPayload(false, "Topic does not exists");

        switch (message.getMode()) {
            case "producer":
                if (topicsDriver.isTopicProducer(client, topicName)) {
                    topicsDriver.unregisterTopic(topicName);
                    return createFeedbackPayload(true, "Successfully unregistered topic: " + topicName);
                }
                break;

            case "subscriber":
                if (topicsDriver.isTopicSubscriber(client, topicName)) {
                    topicsDriver.unregisterSubscription(topicName, client);
                    return createFeedbackPayload(true, "Successfully unsubscribed topic: " + topicName);
                }
                break;
        }

        return createFeedbackPayload(false, "You have nothing in common with given topic");
    }



    /* CONFIG */

    private ConfigResponse config(Message message) {
        ConfigPayload payload = (ConfigPayload) message.getPayload();

        Optional<String> errorMessage = Validator.validatePayload(payload, ConfigPayload.class);
        if (errorMessage.isPresent())
            return new ConfigResponse(errorMessage.get(), false);

        try {
            Config config = serverController.getServerConfig();
            String configJson = mapper.writeValueAsString(config);
            return new ConfigResponse(configJson, true);

//            message.setPayload(configResponse);
//            String responseJson = mapper.writeValueAsString(message);
//            configResponse.setConfig(responseJson);
//
//            return configResponse;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ConfigResponse("Unexpected error", false);
        }
    }

}
