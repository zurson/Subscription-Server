package org.example.server.receive_message.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.example.server.receive_message.Payload;

@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ConfigResponse extends Payload {

    private String config;

    public ConfigResponse(String config, boolean success) {
        this.config = config;
        setSuccess(success);
    }

}
