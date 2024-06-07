package org.example.interfaces;

import org.example.server.topics.TopicData;

import java.util.Map;

public interface ServerController {

    Map<String, TopicData> getTopics();

}
