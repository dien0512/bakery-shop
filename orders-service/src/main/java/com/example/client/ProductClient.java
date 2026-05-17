package com.example.client;

import com.example.dto.request.StockAdjustRequest;
import com.example.dto.response.ProductResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "product-service")
@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ProductClient {

    @GET
    @Path("/{id}")
    ProductResponse getProduct(@PathParam("id") String id);

    // THÊM MỚI
    @PATCH
    @Path("/{id}/stock/internal")
    ProductResponse adjustStock(@PathParam("id") String id, StockAdjustRequest request);
}