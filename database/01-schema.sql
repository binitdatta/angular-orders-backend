CREATE DATABASE IF NOT EXISTS internship_orders CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE internship_orders;

CREATE TABLE IF NOT EXISTS products (
 id BIGINT NOT NULL PRIMARY KEY,
 sku VARCHAR(64) NOT NULL UNIQUE,
 name VARCHAR(160) NOT NULL,
 price DECIMAL(12,2) NOT NULL,
 stock_quantity INT NOT NULL,
 CONSTRAINT chk_products_price CHECK (price >= 0),
 CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer_orders (
 id CHAR(36) NOT NULL PRIMARY KEY,
 customer_subject VARCHAR(100) NOT NULL,
 status VARCHAR(24) NOT NULL,
 total_amount DECIMAL(14,2) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_orders_subject_created (customer_subject, created_at),
 CONSTRAINT chk_order_total CHECK (total_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_items (
 order_id CHAR(36) NOT NULL,
 product_id BIGINT NOT NULL,
 product_name VARCHAR(160) NOT NULL,
 quantity INT NOT NULL,
 unit_price DECIMAL(12,2) NOT NULL,
 PRIMARY KEY (order_id, product_id),
 CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES customer_orders(id),
 CONSTRAINT fk_item_product FOREIGN KEY (product_id) REFERENCES products(id),
 CONSTRAINT chk_item_qty CHECK (quantity > 0),
 CONSTRAINT chk_item_price CHECK (unit_price >= 0)
) ENGINE=InnoDB;
