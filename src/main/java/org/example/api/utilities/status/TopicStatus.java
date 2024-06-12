package org.example.api.utilities.status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicStatus {

    private String topic;
    private String producer;
    private List<String> subscribers;

}
