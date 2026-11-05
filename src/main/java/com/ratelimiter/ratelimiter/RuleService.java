package com.ratelimiter.ratelimiter;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RuleService {

    private static final long TTL_MS = 60_000;
    private record Cached(RateLimitRule rule, long expiresAt) {}

    private final JdbcTemplate jdbc;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    public RuleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RateLimitRule> find(String clientId) {
        long now = System.currentTimeMillis();
        Cached cached = cache.get(clientId);
        if (cached != null && cached.expiresAt() > now) {
            return Optional.of(cached.rule());
        }
        List<RateLimitRule> rows = jdbc.query(
                "SELECT capacity, refill_per_sec FROM rate_limit_rules WHERE client_id = ?",
                (rs, i) -> new RateLimitRule(rs.getInt("capacity"), rs.getDouble("refill_per_sec")),
                clientId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        cache.put(clientId, new Cached(rows.get(0), now + TTL_MS));
        return Optional.of(rows.get(0));
    }
}