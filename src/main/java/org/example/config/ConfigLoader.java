package org.example.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.InputStream;

@NoArgsConstructor
public class ConfigLoader {

    public Config loadConfig() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        Config config;

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.json")) {
            if (inputStream == null)
                throw new IllegalArgumentException("File not found! resources/config.json");

            config = objectMapper.readValue(inputStream, Config.class);
        }

        return config;
    }

}
