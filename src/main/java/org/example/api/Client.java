package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.api.exceptions.ErrorResponseException;
import org.example.api.exceptions.ValidationException;
import org.example.api.threads.ServerListenerThread;
import org.example.api.utilities.ICallbackDriver;
import org.example.api.utilities.IResponseHandler;
import org.example.api.utilities.Message;
import org.example.api.utilities.Validator;
import org.example.api.utilities.payload.FeedbackPayload;
import org.example.api.utilities.payload.MessagePayload;
import org.example.api.utilities.payload.Payload;
import org.example.api.utilities.status.ClientTopics;
import org.example.api.utilities.status.StatusResponse;
import org.example.api.utilities.status.TopicStatus;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.example.api.settings.Settings.SERVER_CONNECTION_TIMEOUT_MS;


public class Client implements ICallbackDriver, IResponseHandler {

    private final Object lock = new Object();
    private FeedbackPayload feedback;

    private final Map<String, Consumer<Message>> callbacks;

    private final ObjectMapper mapper;
    private DataOutputStream outputStream;
    private ServerListenerThread serverListenerThread;
    private Socket clientSocket;
    private String clientId;


    public Client() {
        callbacks = Collections.synchronizedMap(new HashMap<>());
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }


    /* SEND MESSAGE */


    private FeedbackPayload sendMessage(Message message) throws IOException {
        synchronized (lock) {
            feedback = null;
            String mappedMessage = mapper.writeValueAsString(message);
            byte[] messageBytes = mappedMessage.getBytes(StandardCharsets.UTF_8);

//            System.err.println("Sending: " + mappedMessage);

            outputStream.write(messageBytes);
            outputStream.flush();

            try {
                lock.wait(message.getType().equals("message") ? 1 : 5000);
            } catch (InterruptedException e) {
                feedback = new FeedbackPayload();
                feedback.setMessage("Time expired");
                feedback.setSuccess(false);
            }

            return feedback;
        }
    }


    /* START */


    public void start(String host, int port, String clientId) throws ValidationException, IOException {
        if (alreadyRunning())
            throw new IllegalStateException("Client is already running");

        validateStartParameters(host, port, clientId);
        this.clientId = clientId;

        connectToServer(host, port);
        outputStream = new DataOutputStream(clientSocket.getOutputStream());
        startServerListenerThread();
    }


    private void validateStartParameters(String host, int port, String clientId) throws ValidationException {
        if (!Validator.isValidPort(port))
            throw new ValidationException("Invalid port");

        if (!Validator.isValidIPv4Address(host))
            throw new ValidationException("Invalid hostname");

        if (!Validator.isValidId(clientId))
            throw new ValidationException("Invalid clientId");
    }


    private boolean alreadyRunning() {
        return clientId != null && clientSocket.isConnected();
    }


    private void connectToServer(String host, int port) throws IOException {
        clientSocket = new Socket();
        clientSocket.connect(new InetSocketAddress(host, port), SERVER_CONNECTION_TIMEOUT_MS);
    }


    private void startServerListenerThread() throws IOException {
        serverListenerThread = new ServerListenerThread(clientSocket, this, this);
        serverListenerThread.start();
    }


    /* SERVER COMMUNICATION */


    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected();
    }


    public void getServerStatus(Consumer<Message> callback) throws IOException, ErrorResponseException {
        if (!isConnected())
            throw new IllegalStateException("Client is not connected");

        Message message = new Message(
                "status",
                clientId,
                "logs",
                "producer",
                Instant.now(),
                new Payload() {
                }
        );

        callbacks.put("status", callback);

        FeedbackPayload response = sendMessage(message);
        if (!response.isSuccess())
            throw new ErrorResponseException(response.getMessage());
    }


    public void createProducer(String topicName) throws IOException, ErrorResponseException {
        if (!isConnected())
            throw new IllegalStateException("Client is not connected");

        Message message = new Message(
                "register",
                clientId,
                topicName,
                "producer",
                Instant.now(),
                new Payload() {
                }
        );

        FeedbackPayload response = sendMessage(message);
        if (!response.isSuccess())
            throw new ErrorResponseException(response.getMessage());

        System.out.println(response.getMessage());
    }


    public void withdrawProducer(String topicName) throws IOException, ErrorResponseException {
        if (!isConnected())
            throw new IllegalStateException("Client is not connected");

        Message message = new Message(
                "withdraw",
                clientId,
                topicName,
                "producer",
                Instant.now(),
                new Payload() {
                }
        );

        FeedbackPayload response = sendMessage(message);
        if (!response.isSuccess())
            throw new ErrorResponseException(response.getMessage());
    }


    public void produce(String topicName, MessagePayload payload) throws IOException, ErrorResponseException {
        if (!isConnected())
            throw new IllegalStateException("Client is not connected");

        Message message = new Message(
                "message",
                clientId,
                topicName,
                "producer",
                Instant.now(),
                payload
        );

        /*FeedbackPayload response = */
        sendMessage(message);
//        if (!response.isSuccess())
//            throw new ErrorResponseException(response.getMessage());
    }


    public void createSubscriber(String topicName, Consumer<Message> callback) throws IOException, ErrorResponseException {
        if (!isConnected())
            throw new IllegalStateException("Client is not connected");

        Message message = new Message(
                "register",
                clientId,
                topicName,
                "subscriber",
                Instant.now(),
                new Payload() {
                }
        );

        callbacks.put(topicName, callback);

        FeedbackPayload response = sendMessage(message);
        if (!response.isSuccess())
            throw new ErrorResponseException(response.getMessage());
    }


    public void withdrawSubscriber(String topicName) throws IOException, ErrorResponseException {
        if (!isConnected())
            throw new IllegalStateException("Client is not connected");

        Message message = new Message(
                "withdraw",
                clientId,
                topicName,
                "subscriber",
                Instant.now(),
                new Payload() {
                }
        );

        FeedbackPayload response = sendMessage(message);
        if (!response.isSuccess())
            throw new ErrorResponseException(response.getMessage());
    }


    public void stop() {
        if (clientSocket == null)
            return;

        serverListenerThread.stopThread();
        callbacks.clear();

        try {
            clientSocket.close();
        } catch (Exception ignored) {
        }
    }


    public String getStatus() throws IOException, ErrorResponseException {
        if (!isConnected())
            throw new IllegalStateException("Client is not connected");

        Message message = new Message(
                "status",
                clientId,
                "logs",
                "producer",
                Instant.now(),
                new Payload() {
                }
        );

        FeedbackPayload response = sendMessage(message);
        if (!response.isSuccess())
            throw new ErrorResponseException(response.getMessage());

        String json = preparePayloadForStatusesConversion(response.getMessage());
        StatusResponse statusResponse = mapJsonToClass(json, StatusResponse.class);

        ClientTopics clientTopics = getClientTopics(statusResponse);
        return convertToJson(clientTopics);
    }


    /* UTILITIES */


    private String preparePayloadForStatusesConversion(String data) throws IOException {
        return removeFieldFromJson(data, "success");
    }


    private <T> T mapJsonToClass(String jsonString, Class<T> clazz) throws IOException {
        return mapper.readValue(jsonString, clazz);
    }


    private <T> String convertToJson(T object) throws IOException {
        return mapper.writeValueAsString(object);
    }


    private ClientTopics getClientTopics(StatusResponse statusResponse) {
        ClientTopics clientTopics = new ClientTopics();
        for (TopicStatus topicStatus : statusResponse.getStatuses()) {
            if (topicStatus.getProducer().equals(clientId))
                clientTopics.getProducing().add(topicStatus.getTopic());

            if (topicStatus.getSubscribers().contains(clientId))
                clientTopics.getSubscribing().add(topicStatus.getTopic());
        }

        return clientTopics;
    }

    /* ICallbackDriver */


    @Override
    public Consumer<Message> getCallback(String key) {
        return callbacks.get(key);
    }

    /* IResponseHandler */


    @Override
    public void handleServerResponse(FeedbackPayload feedbackPayload) {
        synchronized (lock) {
            feedback = feedbackPayload;
            lock.notify();
        }
    }


    private String removeFieldFromJson(String json, String fieldName) throws IOException {
        JsonNode jsonNode = mapper.readTree(json);
        if (jsonNode instanceof ObjectNode) {
            ((ObjectNode) jsonNode).remove(fieldName);
        }
        return mapper.writeValueAsString(jsonNode);
    }

}
