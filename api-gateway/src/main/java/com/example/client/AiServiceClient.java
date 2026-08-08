package com.example.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Encoded;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "com.example.gateway.client.AiServiceClient")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/")
public interface AiServiceClient {
    @GET
    @Path("{path: .+}")
    Response get(@PathParam("path") @Encoded String path, @HeaderParam("Authorization") String authorization);

    @POST
    @Path("{path: .+}")
    Response post(@PathParam("path") @Encoded String path, @HeaderParam("Authorization") String authorization, String body);

    @PUT
    @Path("{path: .+}")
    Response put(@PathParam("path") @Encoded String path, @HeaderParam("Authorization") String authorization, String body);

    @PATCH
    @Path("{path: .+}")
    Response patch(@PathParam("path") @Encoded String path, @HeaderParam("Authorization") String authorization, String body);

    @DELETE
    @Path("{path: .+}")
    Response delete(@PathParam("path") @Encoded String path, @HeaderParam("Authorization") String authorization);
}
