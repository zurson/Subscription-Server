package org.example.api.utilities.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
public class FeedbackPayload extends Payload {

    private String message;
    private Instant timestampOfMessage;
    private String topicOfMessage;

}
