package org.example.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Config {

    @JsonProperty("ServerId")
    private String serverId;

    @JsonProperty("ListenAddresses")
    private String listenAddresses;

    @JsonProperty("ListenPort")
    private int listenPort;

    @JsonProperty("TimeOut")
    private int timeOut;

    public String getListenAddresses() {
        if (Objects.equals(listenAddresses, "*"))
            return "0.0.0.0";

        return listenAddresses;
    }
}
