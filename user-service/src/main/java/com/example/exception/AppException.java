package com.example.exception;

import jakarta.ws.rs.core.Response;

public class AppException extends RuntimeException {
    private final int statusCode;

    AppException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}