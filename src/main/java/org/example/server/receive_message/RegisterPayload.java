package org.example.server.receive_message;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterPayload extends Payload {

    @NotEmpty
    private String name;

}
