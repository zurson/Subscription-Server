package org.example.interfaces;

import org.example.client.ClientThread;
import org.example.config.Config;
import org.example.server.topics.TopicData;

import java.util.List;
import java.util.Map;

public interface ServerController {

    Map<String, TopicData> getTopics();

    Object getTopicSynchronizer();

    List<ClientThread> getConnectedClients();

    Config getServerConfig();

    void stopServer();
}
