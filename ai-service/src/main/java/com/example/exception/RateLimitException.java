package com.example.exception;

public class RateLimitException extends AppException {
    public RateLimitException() {
        super(429, "AI_RATE_LIMITED", "Too many AI consultation requests. Please try again later.");
    }
}
