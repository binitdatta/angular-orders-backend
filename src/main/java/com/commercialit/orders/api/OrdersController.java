package com.commercialit.orders.api;

import com.commercialit.orders.dto.OrderDtos.*;
import com.commercialit.orders.service.OrderService;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrdersController {
    private final OrderService orders;

    public OrdersController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/me")
    public Object me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return java.util.Map.of("subject", jwt.getSubject(), "username", jwt.getClaimAsString("preferred_username"));
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public List<ProductResponse> products() {
        return orders.products();
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public List<OrderResponse> mine(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return orders.mine(jwt.getSubject());
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public OrderResponse get(@PathVariable String id, @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return orders.getOne(id, jwt.getSubject(), authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                @org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        OrderResponse result = orders.create(jwt.getSubject(), request);
        return ResponseEntity.created(URI.create("/api/orders/" + result.id())).body(result);
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrderResponse> all() {
        return orders.all();
    }
}
