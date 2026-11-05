CREATE TABLE blocked_requests (
  id         BIGSERIAL PRIMARY KEY,
  client_id  VARCHAR(100) NOT NULL,
  path       VARCHAR(255) NOT NULL,
  blocked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_blocked_requests_client_time ON blocked_requests (client_id, blocked_at DESC);