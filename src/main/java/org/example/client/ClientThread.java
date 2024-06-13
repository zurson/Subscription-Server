package org.example.client;

import lombok.Setter;
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
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.settings.Settings.CLIENT_NOT_CONNECTED_MSG;


public class ClientThread extends Thread {

    private Socket clientSocket;

    private String clientId;

    @Setter
    private boolean actionRequest;

    private DataOutputStream outputStream;
    private DataInputStream inputStream;
    private ClientsListDriver clientsListDriver;
    private ReceiveDriver receiveDriver;
    private TopicsDriver topicsDriver;
    private ServerController serverController;
    private AtomicBoolean running;
    private int sizeLimit;


    public ClientThread(ClientsListDriver clientsListDriver, ReceiveDriver receiveDriver, TopicsDriver topicsDriver,
                        ServerController serverController, Socket clientSocket, int sizeLimit) throws IOException {
        if (clientSocket == null || clientSocket.isClosed())
            throw new IOException("Socket null or closed");

        this.clientsListDriver = clientsListDriver;
        this.receiveDriver = receiveDriver;
        this.clientSocket = clientSocket;
        this.topicsDriver = topicsDriver;
        this.serverController = serverController;

        this.sizeLimit = sizeLimit;

        this.running = new AtomicBoolean(false);
        this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        this.inputStream = new DataInputStream(this.clientSocket.getInputStream());
    }


    public ClientThread(String serverId) {
        clientId = serverId;
    }


    @Override
    public void run() {
        running.set(true);

        while (running.get()) {
            try {
                String recvMessage = receiveMessage();
                receiveDriver.addNewMessage(new ReceivedMessage(recvMessage, this));
            } catch (SocketTimeoutException e) {
                if (actionRequest)
                    continue;
                disconnectClientDueToNoActivity();
                break;
            } catch (IOException e) {
                System.out.println("Client " + clientSocket.getRemoteSocketAddress() + " disconnected (" + clientId + ")");
                disconnect();
                break;
            }
        }
    }


    public synchronized void stopThread() {
        if (clientSocket == null || !running.get())
            return;

        running.set(false);
        try {
            clientSocket.close();
        } catch (Exception ignored) {
        }
    }


    private void disconnectClientDueToNoActivity() {
        System.err.println("Timeout: No data received from client " + clientSocket.getRemoteSocketAddress() + " (" + clientId + ")");
        disconnect();
    }


    public synchronized int sendMessage(String message) throws IOException {
        if (clientSocket == null || !clientSocket.isConnected())
            throw new IOException(CLIENT_NOT_CONNECTED_MSG);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        outputStream.write(messageBytes);
        outputStream.flush();

        return messageBytes.length;
    }


    public String receiveMessage() throws IOException {
        if (clientSocket == null || !clientSocket.isConnected())
            throw new IOException(CLIENT_NOT_CONNECTED_MSG);

        byte[] bytes = new byte[sizeLimit];
        int bytesRead = inputStream.read(bytes);

        if (bytesRead == -1)
            throw new IOException("Client communication error");

        return new String(bytes, 0, bytesRead);
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
        if (clientSocket == null)
            return;

        clientsListDriver.removeClient(this);
        closeStreams();

        Map<String, TopicData> topics = serverController.getTopics();

        for (Map.Entry<String, TopicData> entry : topics.entrySet()) {
            String topicName = entry.getKey();
            TopicData topicData = entry.getValue();
            ClientThread producer = topicData.getProducer();

            if (Objects.equals(producer, this)) {
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
