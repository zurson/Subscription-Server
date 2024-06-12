package org.example;

import org.example.config.Config;
import org.example.config.ConfigLoader;
import org.example.server.Server;

public class Main {

    public static void main(String[] args) {

        try {
            ConfigLoader configLoader = new ConfigLoader();
            Config config = configLoader.loadConfig();

            Server server = new Server(config);
            Thread thread = new Thread(server);
            thread.start();


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}