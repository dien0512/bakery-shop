package com.example.exception;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof AppException ex) {
            return buildResponse(ex.getStatusCode(), ex.getMessage());
        }

        if (exception instanceof UnauthorizedException) {
            return buildResponse(401, "Unauthorized");
        }

        if (exception instanceof ForbiddenException) {
            return buildResponse(403, "Access denied: insufficient permissions");
        }

        if (exception instanceof ConstraintViolationException ex) {
            String message = ex.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
            return buildResponse(400, message);
        }

        LOG.errorf("Unhandled exception: %s", exception.getMessage(), exception);
        return buildResponse(500, "Internal server error");
    }

    private Response buildResponse(int statusCode, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", statusCode);
        body.put("error", resolveError(statusCode));
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now().toString());
        return Response.status(statusCode).entity(body).build();
    }

    private String resolveError(int code) {
        return switch (code) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            default -> "Internal Server Error";
        };
    }
}
