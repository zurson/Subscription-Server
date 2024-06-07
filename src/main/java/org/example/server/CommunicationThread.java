package org.example.server;

import org.example.client.ClientThread;
import org.example.interfaces.ClientsListDriver;
import org.example.interfaces.ReceiveDriver;

import java.io.IOException;
import java.net.*;

public class CommunicationThread extends Thread {

    private final int port;
    private final String hostname;
    private final ServerSocket serverSocket;
    private final ClientsListDriver clientsListDriver;
    private final ReceiveDriver receiveDriver;


    public CommunicationThread(ClientsListDriver clientsListDriver, ReceiveDriver receiveDriver, String hostname, int port, int timeout) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.clientsListDriver = clientsListDriver;
        this.receiveDriver = receiveDriver;

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
        ClientThread clientThread = new ClientThread(clientsListDriver, receiveDriver, clientSocket);
        Thread thread = new Thread(clientThread);
        thread.setDaemon(true);
        thread.start();

        return clientThread;
    }

}
