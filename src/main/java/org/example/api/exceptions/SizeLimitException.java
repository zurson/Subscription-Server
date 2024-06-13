package org.example.api.exceptions;

public class SizeLimitException extends Exception {

    public SizeLimitException() {
    }

    public SizeLimitException(String message) {
        super(message);
    }

}
