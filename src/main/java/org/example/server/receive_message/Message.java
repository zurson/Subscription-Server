package org.example.server.receive_message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Message {
    @NotNull
    @Pattern(regexp = "register|withdraw|message|status|config", message = "Unknown type")
    private String type;

    @NotNull(message = "senderId null")
    private String senderId;

    @NotNull(message = "topic null")
    private String topic;

    @NotNull(message = "mode null")
    @Pattern(regexp = "producer|subscriber", message = "Unknown mode")
    private String mode;

    @NotNull(message = "timestamp null")
    private Instant timestamp;

    @NotNull(message = "payload null")
    private Payload payload;
}
