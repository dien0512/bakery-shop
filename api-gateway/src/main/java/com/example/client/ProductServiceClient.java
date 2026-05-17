package com.example.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "com.example.gateway.client.ProductServiceClient")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/")
public interface ProductServiceClient {

    @GET
    @Path("{path: .+}")
    Response get(@PathParam("path") @Encoded String path,
                 @HeaderParam("Authorization") String authorization);

    @POST
    @Path("{path: .+}")
    Response post(@PathParam("path") @Encoded String path,
                  @HeaderParam("Authorization") String authorization,
                  String body);

    @PUT
    @Path("{path: .+}")
    Response put(@PathParam("path") @Encoded String path,
                 @HeaderParam("Authorization") String authorization,
                 String body);

    @DELETE
    @Path("{path: .+}")
    Response delete(@PathParam("path") @Encoded String path,
                    @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("{path: .+}")
    Response patch(@PathParam("path") @Encoded String path,
                   @HeaderParam("Authorization") String authorization,
                   String body);
}