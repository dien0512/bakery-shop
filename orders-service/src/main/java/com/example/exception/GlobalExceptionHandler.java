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
    public Response toResponse(Exception e) {
        if (e instanceof AppException ex) {
            return Response.status(ex.getStatusCode())
                    .entity(Map.of("error", ex.getMessage()))
                    .build();
        }

        if (e instanceof ConstraintViolationException ex) {
            String errors = ex.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));
            return Response.status(400)
                    .entity(Map.of("error", errors))
                    .build();
        }

        return Response.status(500)
                .entity(Map.of("error", "Internal server error"))
                .build();
    }
}
