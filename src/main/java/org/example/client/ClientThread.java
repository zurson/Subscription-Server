package org.example.client;

import org.example.interfaces.ClientsListDriver;
import org.example.interfaces.ReceiveDriver;
import org.example.interfaces.ServerController;
import org.example.interfaces.TopicsDriver;
import org.example.server.receive_message.ReceivedMessage;
import org.example.server.topics.TopicData;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.settings.Settings.CLIENT_NOT_CONNECTED_MSG;
import static org.example.settings.Settings.MAX_TRANSFER_BYTES;


public class ClientThread extends Thread {

    private final Socket clientSocket;

    private String clientId;

    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;
    private final ClientsListDriver clientsListDriver;
    private final ReceiveDriver receiveDriver;
    private final TopicsDriver topicsDriver;
    private final ServerController serverController;
    private final AtomicBoolean running;


    public ClientThread(ClientsListDriver clientsListDriver, ReceiveDriver receiveDriver, TopicsDriver topicsDriver, ServerController serverController, Socket clientSocket) throws IOException {
        if (clientSocket == null || clientSocket.isClosed())
            throw new IOException("Socket null or closed");

        this.clientsListDriver = clientsListDriver;
        this.receiveDriver = receiveDriver;
        this.clientSocket = clientSocket;
        this.topicsDriver = topicsDriver;
        this.serverController = serverController;

        this.running = new AtomicBoolean(false);
        this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        this.inputStream = new DataInputStream(this.clientSocket.getInputStream());
    }


    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            try {
                String recvMessage = recvMessage();
                receiveDriver.addNewMessage(new ReceivedMessage(recvMessage, this));
            } catch (IOException e) {
                System.out.println("Client " + clientSocket.getRemoteSocketAddress() + " disconnected (" + clientId + ")");
                disconnect();
                break;
            }
        }
    }


    public synchronized void stopThread() {
        if (!running.get())
            return;

        running.set(false);
        try {
            clientSocket.close();
        } catch (Exception ignored) {
        }
    }


    public synchronized int sendMessage(String message) throws IOException {
        if (!clientSocket.isConnected())
            throw new IOException(CLIENT_NOT_CONNECTED_MSG);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        outputStream.write(messageBytes);
        outputStream.flush();

        return messageBytes.length;
    }


    public String recvMessage() throws IOException {
        byte[] bytes = new byte[MAX_TRANSFER_BYTES];
        int bytesRead = inputStream.read(bytes);

        if (bytesRead == -1)
            throw new IOException("Client communication error");

        String recvMessage;
        try {
            recvMessage = new String(bytes, 0, bytesRead);
        } catch (IndexOutOfBoundsException e) {
            throw new IOException(e);
        }

        return recvMessage;
    }


    public synchronized void disconnect() {
        if (clientSocket == null || clientSocket.isClosed())
            return;

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.err.println("Removing client: " + clientId);
            handleClientDisconnect();
        }
    }


    private void handleClientDisconnect() {
        clientsListDriver.removeClient(this);
        closeStreams();

        Map<String, TopicData> topics = serverController.getTopics();

        for (Map.Entry<String, TopicData> entry : topics.entrySet()) {
            String topicName = entry.getKey();
            TopicData topicData = entry.getValue();

            if (topicData.getProducer().equals(this)) {
                topicsDriver.unregisterTopic(topicName);
                continue;
            }

            if (topicData.getSubscribers().contains(this))
                topicsDriver.unregisterSubscription(topicName, this);
        }
    }


    private void closeStreams() {
        try {
            inputStream.close();
            outputStream.close();
        } catch (IOException ignored) {

        }
    }


    public synchronized String getClientId() {
        return clientId;
    }


    public synchronized void setClientId(String clientId) {
        this.clientId = clientId;
    }


    @Override
    public String toString() {
        return clientId;
    }
}
