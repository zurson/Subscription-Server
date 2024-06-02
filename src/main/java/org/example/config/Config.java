package org.example.config;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Config {

    @JsonProperty("serverId")
    private String serverId;


    @Override
    public String toString() {
        return "serverId: " + serverId;
    }
}
