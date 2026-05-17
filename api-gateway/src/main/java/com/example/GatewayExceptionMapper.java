package com.example;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Provider
public class GatewayExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException e) {
        // Forward status + body từ downstream service về client
        if (e.getResponse() != null) {
            return e.getResponse();
        }
        return Response.status(502)
                .entity("{\"error\":\"Bad Gateway\",\"message\":\"" + e.getMessage() + "\"}")
                .build();
    }
}