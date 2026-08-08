package com.example.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof AppException app) {
            return Response.status(app.getStatusCode())
                    .entity(Map.of("status", app.getStatusCode(), "code", app.getCode(), "message", app.getMessage()))
                    .build();
        }
        if (exception instanceof ConstraintViolationException violation) {
            String message = violation.getConstraintViolations().stream()
                    .map(item -> item.getPropertyPath() + ": " + item.getMessage())
                    .collect(Collectors.joining(", "));
            return Response.status(400)
                    .entity(Map.of("status", 400, "code", "VALIDATION_ERROR", "message", message))
                    .build();
        }
        return Response.status(500)
                .entity(Map.of("status", 500, "code", "INTERNAL_ERROR", "message", "Internal server error"))
                .build();
    }
}
