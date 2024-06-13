package org.example.server.receive_message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum FeedbackType {

    REJECT("reject"),
    ACKNOWLEDGE("acknowledge");

    @Getter
    private final String value;
}
