package org.example.client;

import org.example.interfaces.ServerDriver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.example.settings.Settings.CLIENT_NOT_CONNECTED_MSG;
import static org.example.settings.Settings.MAX_TRANSFER_BYTES;

public class ClientThread extends Thread {

    private final Socket clientSocket;
    private final DataOutputStream outputStream;
    private final DataInputStream inputStream;
    private final ServerDriver serverDriver;


    public ClientThread(ServerDriver serverDriver, Socket clientSocket) throws IOException {
        if (clientSocket == null || clientSocket.isClosed())
            throw new IOException("Socket null or closed");

        this.serverDriver = serverDriver;
        this.clientSocket = clientSocket;
        this.outputStream = new DataOutputStream(this.clientSocket.getOutputStream());
        this.inputStream = new DataInputStream(this.clientSocket.getInputStream());
    }


    @Override
    public void run() {
        while (true) {
            try {

                String recvMessage = recvMessage();
                System.out.println(recvMessage);

            } catch (IOException e) {
                e.printStackTrace();
                serverDriver.removeClient(this);
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


    public void disconnect() {
        if (clientSocket == null || clientSocket.isClosed())
            return;

        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverDriver.removeClient(this);
        }
    }

}
