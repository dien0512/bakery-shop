package com.example.exception;

public class AppException extends RuntimeException {
    private final int statusCode;
    private final String code;

    public AppException(int statusCode, String code, String message) {
        super(message);
        this.statusCode = statusCode;
        this.code = code;
    }

    public int getStatusCode() { return statusCode; }
    public String getCode() { return code; }
}
