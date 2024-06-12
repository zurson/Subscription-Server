package org.example.api.utilities.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class MessagePayload extends Payload {

    @NotBlank
    private String message;

}
