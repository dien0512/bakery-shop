package com.example.resource;

import com.example.dto.request.ProductRequest;
import com.example.dto.request.ConsultationFilterRequest;
import com.example.dto.request.StockAdjustRequest;
import com.example.dto.response.PageResponse;
import com.example.dto.response.ProductResponse;
import com.example.service.ProductService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Products")
public class ProductResource {

    @Inject
    ProductService productService;

    @GET
    @Operation(summary = "Get all products (paginated, filterable by category or keyword)")
    public PageResponse<ProductResponse> getProducts(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("categoryId") String categoryId,
            @QueryParam("keyword") String keyword) {
        return productService.getProducts(page, size, categoryId, keyword);
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Get product by ID")
    public ProductResponse getProduct(@PathParam("id") String id) {
        return productService.getProduct(id);
    }

    @POST
    @Path("/internal/candidates")
    @Operation(summary = "[Internal] Find deterministic AI consultation candidates")
    public List<ProductResponse> findConsultationCandidates(ConsultationFilterRequest request) {
        return productService.findConsultationCandidates(request);
    }

    @POST
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Create a new product")
    public Response createProduct(@Valid ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return Response.status(Response.Status.CREATED).entity(product).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Update a product")
    public ProductResponse updateProduct(@PathParam("id") String id, ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Soft delete a product")
    public Response deleteProduct(@PathParam("id") String id) {
        productService.deleteProduct(id);
        return Response.noContent().build();
    }

    @PATCH
    @Path("/{id}/stock")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Adjust product stock (delta can be negative)")
    public ProductResponse adjustStock(@PathParam("id") String id, @Valid StockAdjustRequest request) {
        return productService.adjustStock(id, request.delta);
    }

    // THÊM MỚI: Internal endpoint cho orders-service gọi (không cần auth)
    @PATCH
    @Path("/{id}/stock/internal")
    @Operation(summary = "[Internal] Adjust stock called by other services")
    public ProductResponse adjustStockInternal(@PathParam("id") String id, @Valid StockAdjustRequest request) {
        return productService.adjustStock(id, request.delta);
    }
}
