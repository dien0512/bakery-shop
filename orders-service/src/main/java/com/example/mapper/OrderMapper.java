package com.example.mapper;

import com.example.dto.response.OrderItemResponse;
import com.example.dto.response.OrderResponse;
import com.example.entity.Order;
import com.example.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "cdi")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "subtotal", expression = "java(item.getPrice() != null && item.getQuantity() != null ? item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())) : java.math.BigDecimal.ZERO)")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderResponse> toResponseList(List<Order> orders);
}
