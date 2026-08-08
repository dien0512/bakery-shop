package com.example;

import com.example.client.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GatewayResource {

    @Inject @RestClient UserServiceClient userClient;
    @Inject @RestClient ProductServiceClient productClient;
    @Inject @RestClient OrderServiceClient orderClient;
    @Inject @RestClient NotificationServiceClient notificationClient;
    @Inject @RestClient AiServiceClient aiClient;

    // ═══════════════════════════════════════════
    // USER SERVICE (port 8081)
    // /api/auth/** và /api/users/**
    // ═══════════════════════════════════════════

    @GET    @Path("/auth")              public Response authGetRoot(@HeaderParam("Authorization") String a) { return userClient.get("api/auth", a); }
    @GET    @Path("/auth/{p: .+}")      public Response authGet(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return userClient.get("api/auth/" + p, a); }
    @POST   @Path("/auth")              public Response authPostRoot(@HeaderParam("Authorization") String a, String b) { return userClient.post("api/auth", a, b); }
    @POST   @Path("/auth/{p: .+}")      public Response authPost(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return userClient.post("api/auth/" + p, a, b); }

    @GET    @Path("/users")             public Response usersGetRoot(@HeaderParam("Authorization") String a) { return userClient.get("api/users", a); }
    @GET    @Path("/users/{p: .+}")     public Response usersGet(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return userClient.get("api/users/" + p, a); }
    @POST   @Path("/users")             public Response usersPostRoot(@HeaderParam("Authorization") String a, String b) { return userClient.post("api/users", a, b); }
    @POST   @Path("/users/{p: .+}")     public Response usersPost(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return userClient.post("api/users/" + p, a, b); }
    @PUT    @Path("/users")             public Response usersPutRoot(@HeaderParam("Authorization") String a, String b) { return userClient.put("api/users", a, b); }
    @PUT    @Path("/users/{p: .+}")     public Response usersPut(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return userClient.put("api/users/" + p, a, b); }
    @DELETE @Path("/users")             public Response usersDeleteRoot(@HeaderParam("Authorization") String a) { return userClient.delete("api/users", a); }
    @DELETE @Path("/users/{p: .+}")     public Response usersDelete(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return userClient.delete("api/users/" + p, a); }

    // ═══════════════════════════════════════════
    // PRODUCT SERVICE (port 8082)
    // /api/products/** và /api/categories/**
    // ═══════════════════════════════════════════

    @GET    @Path("/products")              public Response productsGetRoot(@HeaderParam("Authorization") String a) { return productClient.get("api/products", a); }
    @GET    @Path("/products/{p: .+}")      public Response productsGet(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return productClient.get("api/products/" + p, a); }
    @POST   @Path("/products")              public Response productsPostRoot(@HeaderParam("Authorization") String a, String b) { return productClient.post("api/products", a, b); }
    @POST   @Path("/products/{p: .+}")      public Response productsPost(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return productClient.post("api/products/" + p, a, b); }
    @PUT    @Path("/products")              public Response productsPutRoot(@HeaderParam("Authorization") String a, String b) { return productClient.put("api/products", a, b); }
    @PUT    @Path("/products/{p: .+}")      public Response productsPut(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return productClient.put("api/products/" + p, a, b); }
    @DELETE @Path("/products")              public Response productsDeleteRoot(@HeaderParam("Authorization") String a) { return productClient.delete("api/products", a); }
    @DELETE @Path("/products/{p: .+}")      public Response productsDelete(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return productClient.delete("api/products/" + p, a); }

    @GET    @Path("/categories")            public Response categoriesGetRoot(@HeaderParam("Authorization") String a) { return productClient.get("api/categories", a); }
    @GET    @Path("/categories/{p: .+}")    public Response categoriesGet(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return productClient.get("api/categories/" + p, a); }
    @POST   @Path("/categories")            public Response categoriesPostRoot(@HeaderParam("Authorization") String a, String b) { return productClient.post("api/categories", a, b); }
    @POST   @Path("/categories/{p: .+}")    public Response categoriesPost(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return productClient.post("api/categories/" + p, a, b); }
    @PUT    @Path("/categories")            public Response categoriesPutRoot(@HeaderParam("Authorization") String a, String b) { return productClient.put("api/categories", a, b); }
    @PUT    @Path("/categories/{p: .+}")    public Response categoriesPut(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return productClient.put("api/categories/" + p, a, b); }
    @DELETE @Path("/categories")            public Response categoriesDeleteRoot(@HeaderParam("Authorization") String a) { return productClient.delete("api/categories", a); }
    @DELETE @Path("/categories/{p: .+}")    public Response categoriesDelete(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return productClient.delete("api/categories/" + p, a); }

    // ═══════════════════════════════════════════
    // ORDER SERVICE (port 8083)
    // /api/orders/** và /api/payment/**
    // ═══════════════════════════════════════════

    @GET    @Path("/orders")                public Response ordersGetRoot(@HeaderParam("Authorization") String a) { return orderClient.get("api/orders", a); }
    @GET    @Path("/orders/{p: .+}")        public Response ordersGet(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return orderClient.get("api/orders/" + p, a); }
    @POST   @Path("/orders")                public Response ordersPostRoot(@HeaderParam("Authorization") String a, String b) { return orderClient.post("api/orders", a, b); }
    @POST   @Path("/orders/{p: .+}")        public Response ordersPost(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return orderClient.post("api/orders/" + p, a, b); }
    @PUT    @Path("/orders")                public Response ordersPutRoot(@HeaderParam("Authorization") String a, String b) { return orderClient.put("api/orders", a, b); }
    @PUT    @Path("/orders/{p: .+}")        public Response ordersPut(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return orderClient.put("api/orders/" + p, a, b); }
    @PATCH  @Path("/orders")                public Response ordersPatchRoot(@HeaderParam("Authorization") String a, String b) { return orderClient.patch("api/orders", a, b); }
    @PATCH  @Path("/orders/{p: .+}")        public Response ordersPatch(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return orderClient.patch("api/orders/" + p, a, b); }
    @DELETE @Path("/orders")                public Response ordersDeleteRoot(@HeaderParam("Authorization") String a) { return orderClient.delete("api/orders", a); }
    @DELETE @Path("/orders/{p: .+}")        public Response ordersDelete(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return orderClient.delete("api/orders/" + p, a); }

    @GET    @Path("/payment")               public Response paymentGetRoot(@HeaderParam("Authorization") String a) { return orderClient.get("api/payment", a); }
    @GET    @Path("/payment/{p: .+}")       public Response paymentGet(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return orderClient.get("api/payment/" + p, a); }
    @POST   @Path("/payment")               public Response paymentPostRoot(@HeaderParam("Authorization") String a, String b) { return orderClient.post("api/payment", a, b); }
    @POST   @Path("/payment/{p: .+}")       public Response paymentPost(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return orderClient.post("api/payment/" + p, a, b); }

    // ═══════════════════════════════════════════
    // NOTIFICATION SERVICE (port 8084)
    // /api/notifications/**
    // ═══════════════════════════════════════════

    @GET    @Path("/notifications")             public Response notifGetRoot(@HeaderParam("Authorization") String a) { return notificationClient.get("api/notifications", a); }
    @GET    @Path("/notifications/{p: .+}")     public Response notifGet(@PathParam("p") String p, @HeaderParam("Authorization") String a) { return notificationClient.get("api/notifications/" + p, a); }
    @POST   @Path("/notifications")             public Response notifPostRoot(@HeaderParam("Authorization") String a, String b) { return notificationClient.post("api/notifications", a, b); }
    @POST   @Path("/notifications/{p: .+}")     public Response notifPost(@PathParam("p") String p, @HeaderParam("Authorization") String a, String b) { return notificationClient.post("api/notifications/" + p, a, b); }

    // ═══════════════════════════════════════════
    // AI BAKERY CONCIERGE (port 8085)
    // /api/ai/consultations/**
    // ═══════════════════════════════════════════

    @GET    @Path("/ai/consultations/questions")
    public Response aiQuestions(@HeaderParam("Authorization") String a) {
        return aiClient.get("api/ai/consultations/questions", a);
    }

    @POST   @Path("/ai/consultations")
    public Response aiConsult(@HeaderParam("Authorization") String a, String b) {
        return aiClient.post("api/ai/consultations", a, b);
    }
}
