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
        @JsonSubTypes.Type(value = FeedbackPayload.class, name = "register"),
        @JsonSubTypes.Type(value = FeedbackPayload.class, name = "status"),
        @JsonSubTypes.Type(value = FeedbackPayload.class, name = "message"),
        @JsonSubTypes.Type(value = FeedbackPayload.class, name = "withdraw")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
public abstract class Payload {
    private String type;
    private boolean success;
}
