package org.example.client;

import org.example.interfaces.ClientsListDriver;
import org.example.interfaces.ReceiveDriver;
import org.example.server.receive_message.ReceivedMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.example.settings.Settings.CLIENT_NOT_CONNECTED_MSG;
import static org.example.settings.Settings.MAX_TRANSFER_BYTES;


public class ClientThread extends Thread {

    private final Socket clientSocket;

    private String clientId;

    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;
    private final ClientsListDriver clientsListDriver;
    private final ReceiveDriver receiveDriver;


    public ClientThread(ClientsListDriver clientsListDriver, ReceiveDriver receiveDriver, Socket clientSocket) throws IOException {
        if (clientSocket == null || clientSocket.isClosed())
            throw new IOException("Socket null or closed");

        this.clientsListDriver = clientsListDriver;
        this.receiveDriver = receiveDriver;
        this.clientSocket = clientSocket;
        this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        this.inputStream = new DataInputStream(this.clientSocket.getInputStream());
    }


    @Override
    public void run() {
        while (true) {
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


    public int sendMessage(String message) throws IOException {
        if (!clientSocket.isConnected())
            throw new IOException(CLIENT_NOT_CONNECTED_MSG);

        if (message.length() > MAX_TRANSFER_BYTES)
            message = message.substring(0, MAX_TRANSFER_BYTES);

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
        if (clientSocket == null)
            return;

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.err.println("Removing client: " + clientId);
            clientsListDriver.removeClient(this);
            closeStreams();
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
