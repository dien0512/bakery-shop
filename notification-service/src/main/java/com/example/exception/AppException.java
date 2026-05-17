package com.example.exception;

public abstract class AppException extends RuntimeException {
    public AppException(String message) { super(message); }
}
