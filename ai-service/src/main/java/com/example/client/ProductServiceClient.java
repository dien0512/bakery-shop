package com.example.client;

import com.example.dto.ConsultationFilterRequest;
import com.example.dto.ProductResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@RegisterRestClient(configKey = "product-service")
@Path("/api/products/internal")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ProductServiceClient {
    @POST
    @Path("/candidates")
    List<ProductResponse> findCandidates(ConsultationFilterRequest request);
}
