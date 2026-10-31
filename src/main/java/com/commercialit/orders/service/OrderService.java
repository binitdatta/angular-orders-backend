package com.commercialit.orders.service;

import com.commercialit.orders.dto.OrderDtos.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final JdbcTemplate db;
    public OrderService(JdbcTemplate db) { this.db = db; }

    @Transactional(readOnly = true)
    public List<ProductResponse> products() {
        return db.query("SELECT id, sku, name, price, stock_quantity FROM products ORDER BY id",
            (rs, n) -> new ProductResponse(rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
                rs.getBigDecimal("price"), rs.getInt("stock_quantity")));
    }

    @Transactional
    public OrderResponse create(String subject, CreateOrderRequest input) {
        if (input.items().size() > 20) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many order lines");
        var ids = new HashSet<Long>();
        for (OrderLineRequest line : input.items()) {
            if (line.productId() == null || line.quantity() < 1 || line.quantity() > 100 || !ids.add(line.productId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity, product, or duplicate line");
        }
        String id = UUID.randomUUID().toString();
        List<OrderLineResponse> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        // Reserve inventory atomically; failure rolls the entire transaction back.
        for (OrderLineRequest line : input.items()) {
            List<ProductResponse> found = db.query("SELECT id, sku, name, price, stock_quantity FROM products WHERE id = ?",
                (rs, n) -> new ProductResponse(rs.getLong("id"), rs.getString("sku"), rs.getString("name"),
                    rs.getBigDecimal("price"), rs.getInt("stock_quantity")), line.productId());
            if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
            ProductResponse product = found.get(0);
            int changed = db.update("UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?",
                line.quantity(), line.productId(), line.quantity());
            if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient inventory");
            BigDecimal amount = product.price().multiply(BigDecimal.valueOf(line.quantity()));
            total = total.add(amount);
            lines.add(new OrderLineResponse(product.id(), product.name(), line.quantity(), product.price(), amount));
        }
        db.update("INSERT INTO customer_orders(id, customer_subject, status, total_amount) VALUES (?,?,?,?)",
            id, subject, "CREATED", total);
        for (OrderLineResponse line : lines)
            db.update("INSERT INTO order_items(order_id, product_id, product_name, quantity, unit_price) VALUES (?,?,?,?,?)",
                id, line.productId(), line.productName(), line.quantity(), line.unitPrice());
        return new OrderResponse(id, subject, "CREATED", total, lines);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOne(String id, String subject, boolean admin) {
        var headers = db.query("SELECT id, customer_subject, status, total_amount FROM customer_orders WHERE id=?",
            (rs, n) -> new String[]{rs.getString("id"), rs.getString("customer_subject"), rs.getString("status"), rs.getString("total_amount")}, id);
        if (headers.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        String[] h = headers.get(0);
        if (!admin && !h[1].equals(subject)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        return new OrderResponse(h[0], h[1], h[2], new BigDecimal(h[3]), lines(id));
    }
    private List<OrderLineResponse> lines(String id) {
        return db.query("SELECT product_id, product_name, quantity, unit_price FROM order_items WHERE order_id=? ORDER BY product_id",
            (rs, n) -> {
                BigDecimal price=rs.getBigDecimal("unit_price"); int qty=rs.getInt("quantity");
                return new OrderLineResponse(rs.getLong("product_id"),rs.getString("product_name"),qty,price,price.multiply(BigDecimal.valueOf(qty)));
            }, id);
    }
    @Transactional(readOnly = true)
    public List<OrderResponse> mine(String subject) {
        List<String> ids=db.query("SELECT id FROM customer_orders WHERE customer_subject=? ORDER BY created_at DESC",(rs,n)->rs.getString(1),subject);
        return ids.stream().map(id->getOne(id,subject,false)).toList();
    }
    @Transactional(readOnly = true)
    public List<OrderResponse> all() {
        List<String> ids=db.query("SELECT id FROM customer_orders ORDER BY created_at DESC LIMIT 100",(rs,n)->rs.getString(1));
        return ids.stream().map(id->getOne(id,"",true)).toList();
    }
}
