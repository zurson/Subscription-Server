package org.example.server;

import org.example.client.ClientThread;
import org.example.interfaces.ClientsListDriver;
import org.example.interfaces.ReceiveDriver;
import org.example.interfaces.ServerController;
import org.example.interfaces.TopicsDriver;

import java.io.IOException;
import java.net.*;

public class CommunicationThread extends Thread {

    private final int port;
    private final String hostname;
    private final ServerSocket serverSocket;
    private final ClientsListDriver clientsListDriver;
    private final ReceiveDriver receiveDriver;
    private final TopicsDriver topicsDriver;
    private final ServerController serverController;


    public CommunicationThread(ClientsListDriver clientsListDriver, ReceiveDriver receiveDriver, TopicsDriver topicsDriver, ServerController serverController, String hostname, int port, int timeout) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.clientsListDriver = clientsListDriver;
        this.receiveDriver = receiveDriver;
        this.topicsDriver = topicsDriver;
        this.serverController = serverController;

        serverSocket = new ServerSocket();
        serverSocket.setSoTimeout(timeout);

        bindSocket();
    }


    @Override
    public void run() {
        listenForConnections();
    }


    private void bindSocket() throws IOException {
        SocketAddress socketPort = new InetSocketAddress(hostname, port);
        serverSocket.bind(socketPort);
    }


    private void listenForConnections() {
        while (true) {
            Socket clientSocket;

            try {
                clientSocket = acceptConnection();
                System.out.println("New client: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
            } catch (SocketTimeoutException ignored) {
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            try {
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
        thread.setDaemon(true);
        thread.start();

        return clientThread;
    }

}
