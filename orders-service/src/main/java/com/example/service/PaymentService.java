package com.example.service;

import com.example.client.UserClient;
import com.example.dto.event.NotificationEvent;
import com.example.entity.Order;
import com.example.exception.BadRequestException;
import com.example.exception.NotFoundException;
import com.example.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject VnpayService vnpayService;
    @Inject NotificationProducerService notificationProducer;
    @Inject OrderRepository orderRepository;
    @Inject JsonWebToken jwt;

    @Inject
    @RestClient
    UserClient userClient;

    // ── Tạo URL thanh toán VNPAY ─────────────────────────────────────────────

    public String createVnpayUrl(String orderId, String ipAddress) {
        String userId = jwt.getSubject();

        Order order = orderRepository.findByIdOptional(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("Access denied");
        }
        if (order.getPaymentMethod() != Order.PaymentMethod.VNPAY) {
            throw new BadRequestException("Order payment method is not VNPAY");
        }
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is cancelled");
        }

        long amountVnd = order.getTotalPrice().longValue();
        String orderInfo = "Thanh toan don hang " + orderId;

        String url = vnpayService.createPaymentUrl(orderId, amountVnd, orderInfo, ipAddress);
        LOG.infof("VNPAY URL created for order %s", orderId);
        return url;
    }

    // ── VNPAY redirect về sau khi user thanh toán ────────────────────────────

    @Transactional
    public String handleReturn(Map<String, String> params) {
        String orderId = params.get("vnp_TxnRef");

        if (!vnpayService.verifySignature(params)) {
            LOG.warnf("Invalid VNPAY signature for order %s", orderId);
            if (orderId != null) markFailed(orderId);
            return orderId != null ? orderId : "unknown";
        }

        if (vnpayService.isPaymentSuccess(params)) {
            markPaid(orderId);
            LOG.infof("Payment SUCCESS via return URL for order %s", orderId);

            orderRepository.findByIdOptional(orderId).ifPresent(order ->
                    notificationProducer.sendOrderPaid(
                            buildPaymentEvent(order, orderId, params, "ORDER_PAID")
                    )
            );
        } else {
            markFailed(orderId);
            LOG.warnf("Payment FAILED via return URL for order %s, code=%s",
                    orderId, params.get("vnp_ResponseCode"));

            orderRepository.findByIdOptional(orderId).ifPresent(order ->
                    notificationProducer.sendOrderPaid(
                            buildPaymentEvent(order, orderId, params, "PAYMENT_FAILED")
                    )
            );
        }

        return orderId;
    }

    // ── VNPAY IPN — server gọi ngầm để xác nhận giao dịch ───────────────────

    @Transactional
    public String handleIpn(Map<String, String> params) {
        String orderId = params.get("vnp_TxnRef");

        if (!vnpayService.verifySignature(params)) {
            LOG.warnf("IPN: invalid signature for order %s", orderId);
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}";
        }

        if (orderId == null || orderId.isBlank()) {
            return "{\"RspCode\":\"01\",\"Message\":\"Missing order reference\"}";
        }

        Order order = orderRepository.findByIdOptional(orderId).orElse(null);
        if (order == null) {
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
        }

        if (vnpayService.isPaymentSuccess(params)) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setStatus(Order.OrderStatus.CONFIRMED);
            LOG.infof("IPN: payment confirmed for order %s", orderId);

            notificationProducer.sendOrderPaid(
                    buildPaymentEvent(order, orderId, params, "ORDER_PAID")
            );
        } else {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            LOG.warnf("IPN: payment failed for order %s", orderId);
        }

        return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
    }

    // ── Helper: build NotificationEvent cho payment ──────────────────────────

    private NotificationEvent buildPaymentEvent(Order order, String orderId,
                                                Map<String, String> params, String type) {
        String email = null;
        String phone = null;
        try {
            var contact = userClient.getContact(order.getUserId());
            email = contact.getEmail();
            phone = contact.getPhone();
        } catch (Exception e) {
            LOG.warnf("Cannot fetch user contact for userId=%s: %s", order.getUserId(), e.getMessage());
        }

        return NotificationEvent.builder()
                .type(type)
                .userId(order.getUserId())
                .recipientEmail(email)
                .recipientPhone(phone)
                .channels(List.of("EMAIL"))
                .data(Map.of(
                        "orderId", orderId,
                        "totalPrice", order.getTotalPrice(),
                        "transactionId", params.getOrDefault("vnp_TransactionNo", "")
                ))
                .build();
    }

    // ── Helpers: cập nhật trạng thái thanh toán ──────────────────────────────

    private void markPaid(String orderId) {
        orderRepository.findByIdOptional(orderId).ifPresent(o -> {
            if (o.getPaymentStatus() != Order.PaymentStatus.PAID) {
                o.setPaymentStatus(Order.PaymentStatus.PAID);
                o.setStatus(Order.OrderStatus.CONFIRMED);
            }
        });
    }

    private void markFailed(String orderId) {
        orderRepository.findByIdOptional(orderId).ifPresent(o -> {
            if (o.getPaymentStatus() != Order.PaymentStatus.PAID) {
                o.setPaymentStatus(Order.PaymentStatus.FAILED);
            }
        });
    }
}