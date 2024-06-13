package org.example.api.utilities.payload;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FeedbackPayload.class, name = "acknowledge"),
        @JsonSubTypes.Type(value = FeedbackPayload.class, name = "reject"),
        @JsonSubTypes.Type(value = MessagePayload.class, name = "message"),
        @JsonSubTypes.Type(value = FeedbackPayload.class, name = "config"),
        @JsonSubTypes.Type(value = MessagePayload.class, name = "file")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public abstract class Payload {
    private String type;
    private boolean success;
}
