package org.example.server.receive_message.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.server.receive_message.Payload;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
public class MessagePayload extends Payload {

    @NotBlank
    private String message;

}
