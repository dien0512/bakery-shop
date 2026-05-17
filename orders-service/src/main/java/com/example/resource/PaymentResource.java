package com.example.resource;

import com.example.service.PaymentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Path("/api/payment")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payment - VNPAY")
public class PaymentResource {

    @Inject
    PaymentService paymentService;

    @ConfigProperty(name = "app.frontend.url", defaultValue = "http://localhost:3000")
    String frontendUrl;

    /**
     * Bước 1: User gọi endpoint này để lấy URL redirect sang VNPAY.
     * Test trên Postman: copy URL trả về, dán vào browser.
     */
    @GET
    @Path("/vnpay/create/{orderId}")
    @RolesAllowed({"USER", "ADMIN"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Tạo URL thanh toán VNPAY cho đơn hàng")
    public Response createPaymentUrl(
            @PathParam("orderId") String orderId,
            @Context HttpHeaders headers) {

        // Lấy IP client (qua gateway thì lấy X-Forwarded-For)
        String ip = headers.getHeaderString("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = "127.0.0.1";

        String paymentUrl = paymentService.createVnpayUrl(orderId, ip);
        return Response.ok(Map.of("paymentUrl", paymentUrl)).build();
    }

    /**
     * Bước 2: VNPAY redirect browser về đây sau khi user thanh toán xong.
     * VNPAY gắn các params vào query string (GET).
     * Sau khi xử lý, redirect về frontend.
     */
    @GET
    @Path("/vnpay-return")
    @Operation(summary = "VNPAY redirect về sau thanh toán (return URL)")
    public Response vnpayReturn(@Context jakarta.ws.rs.core.UriInfo uriInfo) {
        Map<String, String> params = extractQueryParams(uriInfo);
        String orderId = paymentService.handleReturn(params);

        // Redirect về frontend — cấu hình app.frontend.url trong application.properties
        String redirectUrl = frontendUrl + "/orders/" + orderId + "/result"
                + "?status=" + params.getOrDefault("vnp_ResponseCode", "99");

        return Response.temporaryRedirect(URI.create(redirectUrl))
                .header("ngrok-skip-browser-warning", "true") // bỏ warning page khi chạy qua ngrok
                .build();
    }

    /**
     * Bước 3: VNPAY server gọi ngầm đến đây để xác nhận giao dịch (IPN).
     * Không qua browser — VNPAY POST/GET trực tiếp.
     * Phải trả về JSON chuẩn của VNPAY.
     */
    @GET
    @Path("/vnpay-ipn")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "VNPAY IPN - server-to-server callback")
    public Response vnpayIpn(@Context jakarta.ws.rs.core.UriInfo uriInfo) {
        Map<String, String> params = extractQueryParams(uriInfo);
        String result = paymentService.handleIpn(params);
        return Response.ok(result).build();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private Map<String, String> extractQueryParams(jakarta.ws.rs.core.UriInfo uriInfo) {
        Map<String, String> params = new HashMap<>();
        uriInfo.getQueryParameters().forEach((k, v) -> {
            if (!v.isEmpty()) params.put(k, v.get(0));
        });
        return params;
    }
}