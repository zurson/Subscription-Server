package org.example.server.receive_message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.example.server.receive_message.register.RegisterPayload;
import org.example.server.receive_message.status.StatusPayload;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegisterPayload.class, name = "register"),
        @JsonSubTypes.Type(value = StatusPayload.class, name = "status")
})
@Data
public abstract class Payload {
    private String type;
}