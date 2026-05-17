package com.example.service;

import com.example.client.ProductClient;
import com.example.client.UserClient;
import com.example.dto.event.NotificationEvent;
import com.example.dto.request.OrderItemRequest;
import com.example.dto.request.OrderRequest;
import com.example.dto.request.StockAdjustRequest;
import com.example.dto.request.UpdateStatusRequest;
import com.example.dto.response.OrderResponse;
import com.example.dto.response.PageResponse;
import com.example.dto.response.ProductResponse;
import com.example.entity.Order;
import com.example.entity.OrderItem;
import com.example.exception.BadRequestException;
import com.example.exception.ForbiddenException;
import com.example.exception.NotFoundException;
import com.example.mapper.OrderMapper;
import com.example.repository.OrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject OrderRepository orderRepository;
    @Inject OrderMapper orderMapper;
    @Inject JsonWebToken jwt;
    @Inject NotificationProducerService notificationProducer;

    @Inject
    @RestClient
    ProductClient productClient;

    @Inject
    @RestClient
    UserClient userClient;

    // ── User: tạo đơn hàng ───────────────────────────────────────────────────

    @Transactional
    public OrderResponse createOrder(OrderRequest req) {
        String userId = jwt.getSubject();

        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : req.items) {
            ProductResponse product;
            try {
                product = productClient.getProduct(itemReq.productId);
            } catch (Exception e) {
                throw new BadRequestException("Product not found: " + itemReq.productId);
            }

            if ("OUT_OF_STOCK".equals(product.status) || "DISABLED".equals(product.status)) {
                throw new BadRequestException("Product '" + product.name + "' is not available");
            }
            if (product.stock < itemReq.quantity) {
                throw new BadRequestException("Insufficient stock for product '" + product.name +
                        "'. Available: " + product.stock);
            }

            BigDecimal subtotal = product.price.multiply(BigDecimal.valueOf(itemReq.quantity));
            total = total.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .id(UUID.randomUUID().toString())
                    .productId(product.id)
                    .productName(product.name)
                    .price(product.price)
                    .quantity(itemReq.quantity)
                    .build();
            items.add(item);
        }

        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .totalPrice(total)
                .paymentMethod(req.paymentMethod)
                .shippingAddress(req.shippingAddress)
                .receiverName(req.receiverName)
                .receiverPhone(req.receiverPhone)
                .note(req.note)
                .build();

        items.forEach(item -> item.setOrder(order));
        order.setItems(items);

        orderRepository.persist(order);
        orderRepository.flush();

        // ── Trừ stock sau khi tạo đơn thành công ───────────────────────────
        for (OrderItem item : items) {
            try {
                productClient.adjustStock(item.getProductId(), new StockAdjustRequest(-item.getQuantity()));
                LOG.infof("Stock reduced for product %s by %d", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                LOG.errorf("Failed to reduce stock for product %s: %s", item.getProductId(), e.getMessage());
            }
        }

        LOG.infof("Order created: %s by user %s", order.getId(), userId);

        notificationProducer.sendOrderCreated(
                baseEvent(userId)
                        .type("ORDER_CREATED")
                        .data(Map.of(
                                "orderId", order.getId(),
                                "totalPrice", order.getTotalPrice(),
                                "paymentMethod", order.getPaymentMethod().name(),
                                "receiverName", order.getReceiverName() != null ? order.getReceiverName() : "",
                                "itemCount", order.getItems().size()
                        ))
                        .build()
        );

        return orderMapper.toResponse(order);
    }

    // ── User: xem đơn hàng của mình ─────────────────────────────────────────

    public PageResponse<OrderResponse> getMyOrders(int page, int size) {
        String userId = jwt.getSubject();
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        List<Order> orders = Order.findByUserId(userId, page, size);
        long total = Order.countByUserId(userId);
        return new PageResponse<>(orderMapper.toResponseList(orders), page, size, total);
    }

    public OrderResponse getMyOrder(String orderId) {
        String userId = jwt.getSubject();
        Order order = orderRepository.findByIdOptional(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        return orderMapper.toResponse(order);
    }

    // ── User: huỷ đơn hàng ───────────────────────────────────────────────────

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        String userId = jwt.getSubject();
        Order order = orderRepository.findByIdOptional(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        if (!order.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new BadRequestException("Only PENDING orders can be cancelled");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        LOG.infof("Order cancelled by user: %s", orderId);

        for (OrderItem item : order.getItems()) {
            try {
                productClient.adjustStock(item.getProductId(), new StockAdjustRequest(item.getQuantity()));
                LOG.infof("Stock restored for product %s by %d", item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                LOG.errorf("Failed to restore stock for product %s: %s", item.getProductId(), e.getMessage());
            }
        }

        notificationProducer.sendOrderCancelled(
                baseEvent(userId)
                        .type("ORDER_CANCELLED")
                        .data(Map.of(
                                "orderId", orderId,
                                "totalPrice", order.getTotalPrice()
                        ))
                        .build()
        );

        return orderMapper.toResponse(order);
    }

    // ── Admin: xem tất cả đơn hàng ───────────────────────────────────────────

    public PageResponse<OrderResponse> getAllOrders(int page, int size, String status) {
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        List<Order> orders;
        long total;

        if (status != null && !status.isBlank()) {
            Order.OrderStatus orderStatus;
            try {
                orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
            orders = Order.findByStatus(orderStatus, page, size);
            total = Order.countByStatus(orderStatus);
        } else {
            orders = Order.findAllPaged(page, size);
            total = Order.countAll();
        }

        return new PageResponse<>(orderMapper.toResponseList(orders), page, size, total);
    }

    public OrderResponse getOrderById(String orderId) {
        return orderRepository.findByIdOptional(orderId)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    // ── Admin: cập nhật trạng thái ───────────────────────────────────────────

    @Transactional
    public OrderResponse updateStatus(String orderId, UpdateStatusRequest req) {
        Order order = orderRepository.findByIdOptional(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));

        Order.OrderStatus oldStatus = order.getStatus();
        validateStatusTransition(oldStatus, req.status);
        order.setStatus(req.status);

        if (req.status == Order.OrderStatus.COMPLETED
                && order.getPaymentMethod() == Order.PaymentMethod.COD) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
        }

        if (req.status == Order.OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                try {
                    productClient.adjustStock(item.getProductId(), new StockAdjustRequest(item.getQuantity()));
                    LOG.infof("Stock restored (admin cancel) for product %s by %d", item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    LOG.errorf("Failed to restore stock for product %s: %s", item.getProductId(), e.getMessage());
                }
            }
        }

        LOG.infof("Order %s status updated to %s", orderId, req.status);

        notificationProducer.sendOrderStatusUpdated(
                baseEvent(order.getUserId())
                        .type("ORDER_STATUS_UPDATED")
                        .data(Map.of(
                                "orderId", orderId,
                                "oldStatus", oldStatus.name(),
                                "newStatus", req.status.name(),
                                "totalPrice", order.getTotalPrice()
                        ))
                        .build()
        );

        return orderMapper.toResponse(order);
    }

    // ── Helper: build NotificationEvent với email/phone của user ─────────────

    private NotificationEvent.NotificationEventBuilder baseEvent(String userId) {
        String email = null;
        String phone = null;
        try {
            var contact = userClient.getContact(userId);
            email = contact.getEmail();
            phone = contact.getPhone();
        } catch (Exception e) {
            LOG.warnf("Cannot fetch user contact for userId=%s: %s", userId, e.getMessage());
        }
        return NotificationEvent.builder()
                .userId(userId)
                .recipientEmail(email)
                .recipientPhone(phone)
                .channels(List.of("EMAIL"));
    }

    // ── Validate chuyển trạng thái hợp lệ ───────────────────────────────────

    private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING    -> next == Order.OrderStatus.CONFIRMED || next == Order.OrderStatus.CANCELLED;
            case CONFIRMED  -> next == Order.OrderStatus.SHIPPING  || next == Order.OrderStatus.CANCELLED;
            case SHIPPING   -> next == Order.OrderStatus.COMPLETED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new BadRequestException("Cannot transition from " + current + " to " + next);
        }
    }
}