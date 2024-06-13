package org.example.api.utilities.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
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

    @JsonProperty("SizeLimit")
    private int sizeLimit;

    @JsonProperty("AllowedIPAddresses")
    private List<String> allowedIPAddresses;


    public String getListenAddresses() {
        if (Objects.equals(listenAddresses, "*"))
            return "0.0.0.0";

        return listenAddresses;
    }


    public List<String> getAllowedIPAddresses() {
        if (allowedIPAddresses.contains("any"))
            return Collections.emptyList();

        return allowedIPAddresses;
    }
}