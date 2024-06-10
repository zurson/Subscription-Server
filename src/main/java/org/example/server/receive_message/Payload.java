package org.example.server.receive_message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.example.server.receive_message.message.MessagePayload;
import org.example.server.receive_message.register.RegisterPayload;
import org.example.server.receive_message.status.StatusPayload;

import javax.validation.constraints.NotBlank;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegisterPayload.class, name = "register"),
        @JsonSubTypes.Type(value = StatusPayload.class, name = "status"),
        @JsonSubTypes.Type(value =  MessagePayload.class, name = "message")
})
@Data
public abstract class Payload {
    private String type;
}