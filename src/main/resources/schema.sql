CREATE TABLE IF NOT EXISTS orders (
    id          VARCHAR(36)    NOT NULL PRIMARY KEY,
    customer_id VARCHAR(100)   NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    status      VARCHAR(50)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders (created_at DESC);
