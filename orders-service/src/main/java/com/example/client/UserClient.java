package com.example.client;

import com.example.dto.response.UserContactResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "user-service")
@Path("/api/users")
public interface UserClient {

    @GET
    @Path("/{id}/contact")
    @Produces(MediaType.APPLICATION_JSON)
    UserContactResponse getContact(@PathParam("id") String userId);
}