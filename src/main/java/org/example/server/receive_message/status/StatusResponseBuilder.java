package org.example.server.receive_message.status;

import lombok.Getter;
import org.example.client.ClientThread;
import org.example.server.topics.TopicData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class StatusResponseBuilder {

    public StatusResponseBuilder() {
    }


    public StatusResponsePayload build(Map<String, TopicData> topics) {
        List<TopicStatus> topicStatusResponse = new ArrayList<>();

        for (Map.Entry<String, TopicData> entry : topics.entrySet()) {
            String topic = entry.getKey();
            List<String> subscribers = entry.getValue().getSubscribers().stream().map(ClientThread::toString).toList();
            topicStatusResponse.add(new TopicStatus(topic, subscribers));
        }

        return new StatusResponsePayload(topicStatusResponse);
    }

}
