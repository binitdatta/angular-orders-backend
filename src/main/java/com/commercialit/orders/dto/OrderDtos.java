package com.commercialit.orders.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public final class OrderDtos {
    private OrderDtos() {}
    public record ProductResponse(long id, String sku, String name, BigDecimal price, int stockQuantity) {}
    public record OrderLineRequest(@NotNull Long productId, @Min(1) int quantity) {}
    public record CreateOrderRequest(@NotEmpty List<@Valid OrderLineRequest> items) {}
    public record OrderLineResponse(long productId, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record OrderResponse(String id, String customerSubject, String status, BigDecimal totalAmount, List<OrderLineResponse> lines) {}
}
