package org.example.server.receive_message.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class TopicStatus {

    private String topic;
    private List<String> subscribers;

}
