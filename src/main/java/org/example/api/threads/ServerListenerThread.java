package org.example.api.threads;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.api.utilities.ICallbackDriver;
import org.example.api.utilities.IResponseHandler;
import org.example.api.utilities.Message;
import org.example.api.utilities.Validator;
import org.example.api.utilities.payload.FeedbackPayload;

import javax.validation.ConstraintViolation;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ServerListenerThread extends Thread {

    private final ObjectMapper mapper;
    private final ICallbackDriver callbackDriver;
    private final IResponseHandler responseHandler;
    private final BlockingQueue<String> responseQueue;

    private final Socket clientSocket;
    private final DataInputStream inputStream;
    private final AtomicBoolean running;


    public ServerListenerThread(Socket clientSocket, ICallbackDriver callbackDriver, IResponseHandler responseHandler) throws IOException {
        this.clientSocket = clientSocket;
        this.inputStream = new DataInputStream(clientSocket.getInputStream());
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.callbackDriver = callbackDriver;
        this.responseHandler = responseHandler;
        this.running = new AtomicBoolean(false);

        responseQueue = new LinkedBlockingQueue<>();
        startQueueListenerThread();
    }


    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            try {
                String message = receiveMessage();
                responseQueue.add(message);
            } catch (IOException e) {
                e.printStackTrace();
                stopThread();
            }
        }
    }


    private void manageMessage(String receivedMessage) {
//        System.err.println("RECEIVED: " + receivedMessage);
        Message message = mapReceivedMessage(receivedMessage);

        if (message == null) {
            System.err.println("\nInvalid message received: " + receivedMessage);
            return;
        }

        responseHandler.notifySubscription(message);

        if (!message.getType().equals("message"))
            responseHandler.addNewResponse((FeedbackPayload) message.getPayload());
    }


    private Message mapReceivedMessage(String receivedMessage) {
        if (receivedMessage == null || receivedMessage.isEmpty())
            return null;

        try {
            receivedMessage = insertTypeToPayload(receivedMessage);
            Message message = mapper.readValue(receivedMessage, Message.class);

            Set<ConstraintViolation<Message>> violations = Validator.validateJsonObject(message);
            if (violations == null)
                return null;

            if (!violations.isEmpty()) {
                violations.forEach(messageConstraintViolation -> System.out.println(messageConstraintViolation.getMessage()));
                return null;
            }

            return message;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }


    private Consumer<Message> getCallback(String key) {
        return callbackDriver.getCallback(key);
    }


    private String insertTypeToPayload(String receivedMessage) throws JsonProcessingException {
        JsonNode rootNode = mapper.readTree(receivedMessage);
        String type = rootNode.path("type").asText();
        ((ObjectNode) rootNode.path("payload")).put("type", type);

        return mapper.writeValueAsString(rootNode);
    }


    public String receiveMessage() throws IOException {
        byte[] bytes = new byte[200000];
        int bytesRead = inputStream.read(bytes);

        if (bytesRead == -1)
            throw new IOException("Server communication error");

        return new String(bytes, 0, bytesRead);
    }


    private void startQueueListenerThread() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    String message = responseQueue.poll();
                    if (message == null) {
                        Thread.sleep(1);
                        continue;
                    }

                    manageMessage(message);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        thread.setDaemon(true);
        thread.start();
    }


    public synchronized void stopThread() {
        if (!running.get())
            return;

        running.set(false);

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
