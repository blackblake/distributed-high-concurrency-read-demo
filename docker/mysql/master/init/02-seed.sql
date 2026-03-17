USE hw1;

INSERT INTO product (name, price, stock, description, status)
VALUES
    ('Mechanical Keyboard', 499.00, 80, 'Hot-sale keyboard for cache demo', 'ONLINE'),
    ('Wireless Mouse', 199.00, 120, 'Low latency office mouse', 'ONLINE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    price = VALUES(price),
    stock = VALUES(stock),
    description = VALUES(description),
    status = VALUES(status);
