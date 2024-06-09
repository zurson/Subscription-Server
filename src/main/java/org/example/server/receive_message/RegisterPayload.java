package org.example.server.receive_message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.server.receive_message.register.RegisterAction;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterPayload extends Payload {

    @NotEmpty
    @Pattern(regexp = "create|subscribe", message = "Unknown register action")
    private String action;

    public RegisterAction getRegisterAction() {
        return RegisterAction.valueOf(this.action.toUpperCase());
    }
}
