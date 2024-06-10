package org.example.server.receive_message;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterPayload extends Payload {

//    @NotEmpty
//    @Pattern(regexp = "create|subscribe", message = "Unknown register action")
//    private String action;
//
//    public RegisterAction getRegisterAction() {
//        return RegisterAction.valueOf(this.action.toUpperCase());
//    }
}
