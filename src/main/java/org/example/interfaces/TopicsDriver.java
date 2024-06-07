package org.example.interfaces;

import org.example.server.topics.TopicData;

public interface TopicsDriver {

    void addTopic(String topicName, TopicData topicData);

    void removeTopic(String topicName);

}
