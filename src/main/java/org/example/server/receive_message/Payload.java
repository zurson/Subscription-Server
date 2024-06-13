package org.example.server.receive_message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.example.server.receive_message.config.ConfigPayload;
import org.example.server.receive_message.message.MessagePayload;
import org.example.server.receive_message.register.RegisterPayload;
import org.example.server.receive_message.status.StatusPayload;
import org.example.server.receive_message.withdraw.WithdrawPayload;

import javax.validation.constraints.NotBlank;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RegisterPayload.class, name = "register"),
        @JsonSubTypes.Type(value = StatusPayload.class, name = "status"),
        @JsonSubTypes.Type(value =  MessagePayload.class, name = "message"),
        @JsonSubTypes.Type(value =  WithdrawPayload.class, name = "withdraw"),
        @JsonSubTypes.Type(value =  ConfigPayload.class, name = "config"),
        @JsonSubTypes.Type(value =  MessagePayload.class, name = "file")
})
@Data
public abstract class Payload {
    private String type;
    private boolean success;
}