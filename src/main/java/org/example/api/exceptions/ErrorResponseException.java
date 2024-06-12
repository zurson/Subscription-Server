package org.example.api.exceptions;

public class ErrorResponseException extends Exception {

    public ErrorResponseException() {
    }

    public ErrorResponseException(String message) {
        super(message);
    }

}
