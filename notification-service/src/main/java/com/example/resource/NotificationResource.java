package com.example.resource;

import com.example.dto.request.NotificationRequest;
import com.example.dto.response.NotificationResponse;
import com.example.dto.response.PageResponse;
import com.example.service.NotificationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/api/notifications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Notifications")
public class NotificationResource {

    @Inject NotificationService notificationService;
    @Inject JsonWebToken jwt;

    // ── ADMIN: tạo notification thủ công ─────────────────────────────────────

    @POST
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tạo notification thủ công (ADMIN)")
    public Response create(@Valid NotificationRequest req) {
        NotificationResponse res = notificationService.create(req);
        return Response.status(Response.Status.CREATED).entity(res).build();
    }

    // ── ADMIN: lấy tất cả notifications ──────────────────────────────────────

    @GET
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy tất cả notifications (ADMIN)")
    public Response getAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageResponse<NotificationResponse> result = notificationService.getAll(page, size);
        return Response.ok(result).build();
    }

    // ── ADMIN: lấy notification theo id ──────────────────────────────────────

    @GET
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy notification theo ID (ADMIN)")
    public Response getById(@PathParam("id") String id) {
        return Response.ok(notificationService.getById(id)).build();
    }

    // ── USER: lấy notifications của chính mình ────────────────────────────────

    @GET
    @Path("/my")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy notifications của user hiện tại")
    public Response getMy(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        String userId = jwt.getSubject();
        PageResponse<NotificationResponse> result = notificationService.getByUser(userId, page, size);
        return Response.ok(result).build();
    }

    // ── ADMIN: lấy notifications theo userId ──────────────────────────────────

    @GET
    @Path("/user/{userId}")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Lấy notifications theo userId (ADMIN)")
    public Response getByUser(
            @PathParam("userId") String userId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        PageResponse<NotificationResponse> result = notificationService.getByUser(userId, page, size);
        return Response.ok(result).build();
    }

    // ── ADMIN: trigger retry thủ công ─────────────────────────────────────────

    @POST
    @Path("/retry")
    @RolesAllowed("ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Trigger retry các notification FAILED (ADMIN)")
    public Response triggerRetry() {
        notificationService.retryFailed();
        return Response.ok(Map.of("message", "Retry triggered")).build();
    }
}
