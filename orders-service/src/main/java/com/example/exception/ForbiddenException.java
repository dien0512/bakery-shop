package com.example.exception;
public class ForbiddenException extends AppException {
    public ForbiddenException(String message) { super(message, 403); }
}
