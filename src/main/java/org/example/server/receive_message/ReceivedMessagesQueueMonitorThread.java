package org.example.server.receive_message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.client.ClientThread;
import org.example.interfaces.MessagesQueueDriver;
import org.example.interfaces.TopicsDriver;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.util.Collections;
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

        client.setClientId(message.getSenderId());
        messagesQueueDriver.addMessageToSendQueue("OKEJ", Collections.singletonList(client));

    }


    private Message mapReceivedMessage(String receivedMessage) {
        if (receivedMessage == null || receivedMessage.isEmpty()) {
            return null;
        }

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            String modifiedJson = insertTypeToPayload(receivedMessage);
            Message message = mapper.readValue(modifiedJson, Message.class);

            Validator validator = factory.getValidator();
            Set<ConstraintViolation<Message>> violations = validator.validate(message);

            if (!violations.isEmpty()) {
                violations.forEach(violation -> System.out.println(violation.getMessage()));
                return null;
            }

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



    /* TYPES */

    private void register(Message message) {

    }

}
