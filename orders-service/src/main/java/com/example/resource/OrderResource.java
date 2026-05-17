package com.example.resource;

import com.example.dto.request.OrderRequest;
import com.example.dto.request.UpdateStatusRequest;
import com.example.dto.response.OrderResponse;
import com.example.dto.response.PageResponse;
import com.example.service.OrderService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Orders")
public class OrderResource {

    @Inject
    OrderService orderService;

    // ── USER endpoints ────────────────────────────────────────────────────────

    @POST
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new order")
    public Response createOrder(@Valid OrderRequest request) {
        OrderResponse order = orderService.createOrder(request);
        return Response.status(Response.Status.CREATED).entity(order).build();
    }

    @GET
    @Path("/my")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my orders (paginated)")
    public PageResponse<OrderResponse> getMyOrders(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        return orderService.getMyOrders(page, size);
    }

    @GET
    @Path("/my/{id}")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get my order by ID")
    public OrderResponse getMyOrder(@PathParam("id") String id) {
        return orderService.getMyOrder(id);
    }

    @PATCH
    @Path("/my/{id}/cancel")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancel my order (only PENDING)")
    public OrderResponse cancelOrder(@PathParam("id") String id) {
        return orderService.cancelOrder(id);
    }

    // ── ADMIN endpoints ───────────────────────────────────────────────────────

    @GET
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Get all orders (paginated, filterable by status)")
    public PageResponse<OrderResponse> getAllOrders(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("status") String status) {
        return orderService.getAllOrders(page, size, status);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Get order by ID")
    public OrderResponse getOrderById(@PathParam("id") String id) {
        return orderService.getOrderById(id);
    }

    @PATCH
    @Path("/{id}/status")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "[Admin] Update order status")
    public OrderResponse updateStatus(
            @PathParam("id") String id,
            @Valid UpdateStatusRequest request) {
        return orderService.updateStatus(id, request);
    }
}
