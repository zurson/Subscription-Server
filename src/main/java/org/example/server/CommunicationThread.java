package org.example.server;

import org.apache.commons.net.util.SubnetUtils;
import org.example.client.ClientThread;
import org.example.config.Config;
import org.example.interfaces.ClientsListDriver;
import org.example.interfaces.ReceiveDriver;
import org.example.interfaces.ServerController;
import org.example.interfaces.TopicsDriver;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class CommunicationThread extends Thread {

    private final int port;
    private final String listenAddresses;
    private final int timeout;
    private final int sizeLimit;
    private final List<String> allowedIPAddresses;

    private final ServerSocket serverSocket;
    private final ClientsListDriver clientsListDriver;
    private final ReceiveDriver receiveDriver;
    private final TopicsDriver topicsDriver;
    private final ServerController serverController;
    private final AtomicBoolean running;

    public CommunicationThread(ClientsListDriver clientsListDriver, ReceiveDriver receiveDriver, TopicsDriver topicsDriver, ServerController serverController, Config config) throws IOException {
        this.listenAddresses = config.getListenAddresses();
        this.port = config.getListenPort();
        this.timeout = config.getTimeOut();
        this.sizeLimit = config.getSizeLimit();
        this.allowedIPAddresses = config.getAllowedIPAddresses();

        this.clientsListDriver = clientsListDriver;
        this.receiveDriver = receiveDriver;
        this.topicsDriver = topicsDriver;
        this.serverController = serverController;

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
                System.out.println("New client: " + clientSocket.getRemoteSocketAddress());

                if (!isAllowedIPAddress(clientSocket)) {
                    System.out.println("IP address not allowed: " + clientSocket.getRemoteSocketAddress());
                    clientSocket.close();
                    continue;
                }

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


    private boolean isAllowedIPAddress(Socket clientSocket) {
        if (allowedIPAddresses.isEmpty())
            return true;

        String clientIP = clientSocket.getInetAddress().getHostAddress();

        for (String cidr : allowedIPAddresses) {
            if (isIPInCIDR(clientIP, cidr))
                return true;
        }
        return false;
    }


    private boolean isIPInCIDR(String ip, String cidr) {
        try {
            SubnetUtils subnetUtils = new SubnetUtils(cidr);
            SubnetUtils.SubnetInfo subnetInfo = subnetUtils.getInfo();
            return subnetInfo.isInRange(ip);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    private ClientThread spawnClientThread(Socket clientSocket) throws IOException {
        ClientThread clientThread = new ClientThread(clientsListDriver, receiveDriver, topicsDriver, serverController, clientSocket, sizeLimit);
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
