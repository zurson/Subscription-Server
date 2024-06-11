package org.example.interfaces;

import org.example.client.ClientThread;
import org.example.server.topics.TopicData;

import java.util.Map;

public interface TopicsDriver {

    void addTopic(String topicName, TopicData topicData);

    void removeTopic(String topicName);

    boolean topicExists(String topicName);

    boolean producerExists(String producerId);

    void addSubscriber(String topicName, ClientThread subscriber);

    TopicData getTopic(String topicName);

    boolean isSubscriberOrProducer(String topicNameToSkip, ClientThread client);

    void unregisterTopic(String topicName);

    void unregisterSubscription(String topicName, ClientThread client);

    boolean isTopicProducer(ClientThread client, String topicName);

    boolean isTopicSubscriber(ClientThread client, String topicName);
}
