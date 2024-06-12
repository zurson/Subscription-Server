package org.example.api.utilities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.api.utilities.payload.FeedbackPayload;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ServerResponse {

    private Instant timestamp;
    private FeedbackPayload payload;

}
