package org.example.api.utilities.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.api.utilities.status.TopicStatus;

import javax.validation.constraints.NotNull;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusPayload extends Payload {

    @NotNull(message = "statuses null")
    private List<TopicStatus> statuses;

}
