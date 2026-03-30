CREATE TABLE IF NOT EXISTS orders (
    id          VARCHAR(36)    NOT NULL PRIMARY KEY,
    customer_id VARCHAR(255)   NOT NULL,
    amount      NUMERIC(19, 2) NOT NULL,
    status      VARCHAR(50)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL
);

