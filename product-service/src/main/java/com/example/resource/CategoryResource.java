package com.example.resource;

import com.example.dto.request.CategoryRequest;
import com.example.dto.response.CategoryResponse;
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

@Path("/api/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Categories")
public class CategoryResource {

    @Inject
    ProductService productService;

    @GET
    @Operation(summary = "Get all categories")
    public List<CategoryResponse> getAll() {
        return productService.getAllCategories();
    }

    @POST
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Create category")
    public Response create(@Valid CategoryRequest request) {
        CategoryResponse cat = productService.createCategory(request);
        return Response.status(Response.Status.CREATED).entity(cat).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Update category")
    public CategoryResponse update(@PathParam("id") String id, @Valid CategoryRequest request) {
        return productService.updateCategory(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Delete category")
    public Response delete(@PathParam("id") String id) {
        productService.deleteCategory(id);
        return Response.noContent().build();
    }
}
