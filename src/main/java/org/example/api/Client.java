package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.api.exceptions.ErrorResponseException;
import org.example.api.exceptions.SizeLimitException;
import org.example.api.exceptions.ValidationException;
import org.example.api.threads.ServerListenerThread;
import org.example.api.utilities.*;
import org.example.api.utilities.config.Config;
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
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.example.api.settings.Settings.SERVER_CONNECTION_TIMEOUT_MS;


public class Client implements ICallbackDriver, IResponseHandler {

    private final Object lock = new Object();
    private FeedbackPayload selfStatusPayload;

    private Config serverConfig;
    private final Object configLock = new Object();

    private final Map<String, Consumer<Message>> callbacks;
    private final ServerResponsesQueue<ServerResponse> serverResponses;
    private final Set<Instant> serverRequestsTimestamps;

    private final ObjectMapper mapper;
    private DataOutputStream outputStream;
    private ServerListenerThread serverListenerThread;
    private Socket clientSocket;
    private String clientId;

    private Thread monitorThread;
    private boolean serverLogsEnabled;

    public Client() {
        callbacks = Collections.synchronizedMap(new HashMap<>());
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());

        this.serverResponses = new ServerResponsesQueue<>();
        this.serverRequestsTimestamps = new HashSet<>();

        serverLogsEnabled = false;
    }


    /* SEND MESSAGE */


    private void sendMessage(Message message) throws IOException, SizeLimitException {
        synchronized (lock) {
            String mappedMessage = mapper.writeValueAsString(message);
            byte[] messageBytes = mappedMessage.getBytes(StandardCharsets.UTF_8);

//            System.err.println("Sending: " + mappedMessage);

            if (serverConfig != null && messageBytes.length > serverConfig.getSizeLimit())
                throw new SizeLimitException("Message size limit is " + serverConfig.getSizeLimit() + " your message: " + messageBytes.length);

            outputStream.write(messageBytes);
            outputStream.flush();

            serverRequestsTimestamps.add(message.getTimestamp());

            if (!message.getType().equals("status"))
                return;

            try {
                lock.wait(2000);
            } catch (InterruptedException e) {
            }

            if (selfStatusPayload == null) {
                selfStatusPayload = new FeedbackPayload();
                selfStatusPayload.setMessage("Time expired");
                selfStatusPayload.setSuccess(false);
            }

        }
    }


    /* START */


    public void start(String host, int port, String clientId) throws ValidationException, IOException, SizeLimitException {
        synchronized (configLock) {
            if (alreadyRunning())
                throw new IllegalStateException("Client is already running");

            validateStartParameters(host, port, clientId);
            this.clientId = clientId;

            connectToServer(host, port);
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            startServerListenerThread();

            sendMessage(getConfigMessage());

            try {
                configLock.wait(5000);
            } catch (InterruptedException e) {
            }

            if (serverConfig == null)
                throw new RuntimeException("Unable to get server config");

        }
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


    private Message getConfigMessage() {
        return new Message(
                "config",
                clientId,
                "config",
                "producer",
                Instant.now(),
                new Payload() {
                }
        );
    }


    /* SERVER COMMUNICATION */


    public boolean isConnected() {
        return clientSocket != null && clientSocket.isConnected();
    }


    public void getServerStatus(Consumer<Message> callback) throws IOException, SizeLimitException {
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

        sendMessage(message);
    }


    public void createProducer(String topicName) throws IOException, SizeLimitException {
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

        sendMessage(message);
    }


    public void withdrawProducer(String topicName) throws IOException, SizeLimitException {
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

        sendMessage(message);
    }


    public void produce(String topicName, MessagePayload payload) throws IOException, SizeLimitException {
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

        sendMessage(message);
    }


    public void createSubscriber(String topicName, Consumer<Message> callback) throws IOException, SizeLimitException {
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

        sendMessage(message);
    }


    public void withdrawSubscriber(String topicName) throws IOException, SizeLimitException {
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

        sendMessage(message);
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


    public String getStatus() throws IOException, ErrorResponseException, SizeLimitException {
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

        /*FeedbackPayload response = */

        sendMessage(message);


        if (!selfStatusPayload.isSuccess())
            throw new ErrorResponseException(selfStatusPayload.getMessage());

        String json = preparePayloadForStatusesConversion(selfStatusPayload.getMessage());
        StatusResponse statusResponse = mapJsonToClass(json, StatusResponse.class);

        ClientTopics clientTopics = getClientTopics(statusResponse);

        selfStatusPayload = null;
        return convertToJson(clientTopics);
    }


    public void getServerLogs(BiConsumer<Boolean, String> callback) {
        if (serverLogsEnabled)
            return;

        serverLogsEnabled = true;

        monitorThread = new Thread(() -> {
            while (true) {
                try {
                    ServerResponse serverResponse = serverResponses.poll();

                    if (serverResponse == null) {
                        Thread.sleep(1);
                        continue;
                    }

                    FeedbackPayload payload = serverResponse.getPayload();
                    callback.accept(payload.isSuccess(), payload.getMessage());
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
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
    public void addNewResponse(FeedbackPayload feedbackPayload) {
        serverResponses.add(new ServerResponse(feedbackPayload.getTimestampOfMessage(), feedbackPayload));
//        System.out.println("Server Response: " + feedbackPayload);

        try {
            mapJsonToClass(feedbackPayload.getMessage(), Config.class);
            setConfig(feedbackPayload);
            return;
        } catch (IOException ignored) {
        }

        synchronized (lock) {
            try {
                String json = preparePayloadForStatusesConversion(feedbackPayload.getMessage());
                mapJsonToClass(json, StatusResponse.class);
                selfStatusPayload = feedbackPayload;
                lock.notify();
            } catch (IOException ignored) {
            }
        }
    }


    @Override
    public void notifySubscription(Message message) {
        if (!message.getType().equals("message"))
            return;

        String topicName = message.getTopic();

        Consumer<Message> callback = getCallback(topicName);
        if (callback == null)
            return;

        callback.accept(message);
    }


    public void setConfig(FeedbackPayload feedbackPayload) {
        synchronized (configLock) {
            try {
                serverConfig = mapJsonToClass(feedbackPayload.getMessage(), Config.class);
                System.out.println("Server config loaded");
                configLock.notify();
            } catch (IOException e) {
                throw new RuntimeException("Config mapping error");
            }
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
