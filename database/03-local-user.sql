-- DEVELOPMENT ONLY. Run as a MySQL administrator after schema initialization.
CREATE USER IF NOT EXISTS 'orders_app'@'%' IDENTIFIED BY 'orders_dev_password';
GRANT SELECT, INSERT, UPDATE, DELETE ON internship_orders.* TO 'orders_app'@'%';
