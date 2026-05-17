package com.example.exception;

public class BadRequestException extends AppException{
    public BadRequestException(String message) {
        super(message, 400);
    }
}
