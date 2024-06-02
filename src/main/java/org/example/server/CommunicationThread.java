package org.example.server;

import org.example.client.ClientThread;
import org.example.interfaces.ServerDriver;

import java.io.IOException;
import java.net.*;

public class CommunicationThread extends Thread {

    private final int port;
    private final String hostname;
    private final ServerSocket serverSocket;
    private final ServerDriver serverDriver;


    public CommunicationThread(ServerDriver serverDriver, String hostname, int port, int timeout) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.serverDriver = serverDriver;

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
                serverDriver.addClient(clientThread);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private Socket acceptConnection() throws IOException {
        return serverSocket.accept();
    }


    private ClientThread spawnClientThread(Socket clientSocket) throws IOException {
        ClientThread clientThread = new ClientThread(serverDriver, clientSocket);
        Thread thread = new Thread(clientThread);
        thread.setDaemon(true);
        thread.start();

        return clientThread;
    }

}
