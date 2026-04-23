USE hw1;

-- Seed inventory for the two seeded products (ids 1 and 2 from 02-seed.sql)
INSERT INTO inventory (product_id, total, available, locked, version)
VALUES
    (1, 80, 80, 0, 0),
    (2, 120, 120, 0, 0)
ON DUPLICATE KEY UPDATE
    total = VALUES(total),
    available = VALUES(available),
    locked = 0,
    version = version + 1;
