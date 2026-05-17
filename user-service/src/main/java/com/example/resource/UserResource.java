package com.example.resource;

import com.example.dto.request.AddressRequest;
import com.example.dto.request.UpdateProfileRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.UserResponse;
import com.example.service.UserService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
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

import java.util.List;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    JsonWebToken jwt;

    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "Get current user profile")
    public UserResponse getMyProfile() {
        return userService.getProfile(jwt.getSubject());
    }

    @PUT
    @Path("/me")
    @Authenticated
    @Operation(summary = "Update current user profile")
    public UserResponse updateProfile(UpdateProfileRequest request) {
        return userService.updateProfile(jwt.getSubject(), request);
    }

    @DELETE
    @Path("/me")
    @Authenticated
    @Operation(summary = "Soft delete current user account")
    public Response deleteAccount() {
        userService.deleteAccount(jwt.getSubject());
        return Response.noContent().build();
    }

    // ---- Addresses ----

    @GET
    @Path("/me/addresses")
    @Authenticated
    @Operation(summary = "Get all addresses of current user")
    public List<AddressResponse> getAddresses() {
        return userService.getAddresses(jwt.getSubject());
    }

    @POST
    @Path("/me/addresses")
    @Authenticated
    @Operation(summary = "Add a new address")
    public Response addAddress(@Valid AddressRequest request) {
        AddressResponse address = userService.addAddress(jwt.getSubject(), request);
        return Response.status(Response.Status.CREATED).entity(address).build();
    }

    @DELETE
    @Path("/me/addresses/{addressId}")
    @Authenticated
    @Operation(summary = "Delete an address")
    public Response deleteAddress(@PathParam("addressId") String addressId) {
        userService.deleteAddress(jwt.getSubject(), addressId);
        return Response.noContent().build();
    }

    /**
     * Internal endpoint — cho các service khác gọi để lấy email/phone của user.
     * Chỉ ADMIN hoặc service-to-service call mới được dùng.
     */
    @GET
    @Path("/{id}/contact")
    @PermitAll
    @Operation(summary = "[Internal] Lấy thông tin liên lạc của user")
    public Response getContact(@PathParam("id") String id) {
        return userService.getContact(id);
    }

    // ---- Admin ----

    @GET
    @Path("/{userId}")
    @RolesAllowed("ADMIN")
    @Operation(summary = "[Admin] Get any user by ID")
    public UserResponse getUserById(@PathParam("userId") String userId) {
        return userService.getProfile(userId);
    }
}