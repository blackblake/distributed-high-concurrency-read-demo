USE hw1;

-- Inventory table (independent from product; logically owned by "inventory-service")
CREATE TABLE IF NOT EXISTS inventory (
    product_id BIGINT PRIMARY KEY,
    total      INT NOT NULL DEFAULT 0,
    available  INT NOT NULL DEFAULT 0,
    locked     INT NOT NULL DEFAULT 0,
    version    BIGINT NOT NULL DEFAULT 0,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Inventory idempotency log: one row per processed message
CREATE TABLE IF NOT EXISTS inventory_log (
    message_id VARCHAR(64) PRIMARY KEY,
    product_id BIGINT NOT NULL,
    delta      INT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order table (logically owned by "order-service")
CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    product_id   BIGINT NOT NULL,
    quantity     INT NOT NULL DEFAULT 1,
    amount       DECIMAL(10, 2) NOT NULL,
    status       VARCHAR(32) NOT NULL,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_product (user_id, product_id),
    INDEX idx_user (user_id),
    INDEX idx_create_time (create_time)
);

-- Outbox table (transactional-outbox pattern for reliable message publishing)
CREATE TABLE IF NOT EXISTS outbox_message (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate    VARCHAR(64) NOT NULL,
    topic        VARCHAR(128) NOT NULL,
    msg_key      VARCHAR(128),
    payload      TEXT NOT NULL,
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    retries      INT NOT NULL DEFAULT 0,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    send_time    TIMESTAMP NULL,
    INDEX idx_status_create (status, create_time)
);
