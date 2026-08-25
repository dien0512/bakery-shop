package com.example.exception;

public class BadRequestException extends AppException {
    public BadRequestException(String code, String message) {
        super(400, code, message);
    }
}
