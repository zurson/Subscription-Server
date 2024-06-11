package org.example.server.receive_message.status;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.server.receive_message.Payload;

import java.util.List;

@AllArgsConstructor
@Getter
public class StatusResponsePayload extends Payload {

    private List<TopicStatus> statuses;

}
