CREATE TABLE rate_limit_rules (
  client_id      VARCHAR(100) PRIMARY KEY,
  capacity       INT NOT NULL,
  refill_per_sec DOUBLE PRECISION NOT NULL
);
INSERT INTO rate_limit_rules VALUES ('demo-key', 10, 1);