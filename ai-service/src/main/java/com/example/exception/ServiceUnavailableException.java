package com.example.exception;

public class ServiceUnavailableException extends AppException {
    public ServiceUnavailableException(String code, String message) {
        super(503, code, message);
    }
}
