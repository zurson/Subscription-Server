package org.example.server.receive_message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum FeedbackType {

    REJECT("reject"),
    ACKNOWLEDGE("acknowledge"),
    CONFIG("config")
    ;

    @Getter
    private final String value;
}
