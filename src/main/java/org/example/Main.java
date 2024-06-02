package org.example;

import org.example.server.Server;

public class Main {

    public static void main(String[] args) {

        try {
            Server server = new Server("0.0.0.0", 5000, 3000);

            Thread thread = new Thread(server);
            thread.start();

            Thread.sleep(7000);
            server.stopServer();


//            thread.join();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}