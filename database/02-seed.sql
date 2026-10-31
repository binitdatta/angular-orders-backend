USE internship_orders;
INSERT INTO products(id, sku, name, price, stock_quantity) VALUES
 (101, 'LAPTOP-14', '14-inch Laptop', 899.00, 12),
 (102, 'MOUSE-WL', 'Wireless Mouse', 24.50, 60),
 (103, 'KEYBOARD-USB', 'USB Keyboard', 39.99, 35),
 (104, 'MONITOR-27', '27-inch Monitor', 249.00, 17)
ON DUPLICATE KEY UPDATE name=VALUES(name), price=VALUES(price);

-- The seeded order references Alice's fixed Keycloak user ID (sub).
INSERT IGNORE INTO customer_orders(id, customer_subject, status, total_amount)
VALUES ('00000000-0000-4000-8000-000000000101', '11111111-1111-4111-8111-111111111111', 'CREATED', 923.50);
INSERT IGNORE INTO order_items(order_id, product_id, product_name, quantity, unit_price) VALUES
 ('00000000-0000-4000-8000-000000000101',101,'14-inch Laptop',1,899.00),
 ('00000000-0000-4000-8000-000000000101',102,'Wireless Mouse',1,24.50);
