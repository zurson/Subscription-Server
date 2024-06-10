package org.example.server.receive_message.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class StatusResponse {

    private List<TopicStatus> statuses;

}
