package com.example.exception;
public class ConflictException extends AppException {
    public ConflictException(String message) { super(message, 409); }
}
