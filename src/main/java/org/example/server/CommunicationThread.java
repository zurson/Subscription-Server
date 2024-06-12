package org.example.server;

import org.example.client.ClientThread;
import org.example.interfaces.ClientsListDriver;
import org.example.interfaces.ReceiveDriver;
import org.example.interfaces.ServerController;
import org.example.interfaces.TopicsDriver;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommunicationThread extends Thread {

    private final int port;
    private final String listenAddresses;
    private final ServerSocket serverSocket;
    private final ClientsListDriver clientsListDriver;
    private final ReceiveDriver receiveDriver;
    private final TopicsDriver topicsDriver;
    private final ServerController serverController;
    private final AtomicBoolean running;
    private final int timeout;

    public CommunicationThread(ClientsListDriver clientsListDriver, ReceiveDriver receiveDriver, TopicsDriver topicsDriver, ServerController serverController, String listenAddresses, int port, int timeout) throws IOException {
        this.listenAddresses = listenAddresses;
        this.port = port;
        this.clientsListDriver = clientsListDriver;
        this.receiveDriver = receiveDriver;
        this.topicsDriver = topicsDriver;
        this.serverController = serverController;
        this.timeout = timeout;

        this.running = new AtomicBoolean(false);

        serverSocket = new ServerSocket();
        serverSocket.setSoTimeout(timeout);

        bindSocket();
    }


    @Override
    public void run() {
        running.set(true);
        listenForConnections();
    }


    private void bindSocket() throws IOException {
        SocketAddress socketPort = new InetSocketAddress(listenAddresses, port);
        serverSocket.bind(socketPort);
    }


    private void listenForConnections() {
        while (running.get()) {
            Socket clientSocket;

            try {
                clientSocket = acceptConnection();
                System.out.println("New client: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            } catch (SocketTimeoutException ignored) {
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                stopThread();
                break;
            }

            try {
                clientSocket.setSoTimeout(this.timeout);
                ClientThread clientThread = spawnClientThread(clientSocket);
                clientsListDriver.addClient(clientThread);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private Socket acceptConnection() throws IOException {
        return serverSocket.accept();
    }


    private ClientThread spawnClientThread(Socket clientSocket) throws IOException {
        ClientThread clientThread = new ClientThread(clientsListDriver, receiveDriver, topicsDriver, serverController, clientSocket);
        Thread thread = new Thread(clientThread);
        thread.start();

        return clientThread;
    }


    public synchronized void stopThread() {
        if (!running.get())
            return;

        running.set(false);

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
