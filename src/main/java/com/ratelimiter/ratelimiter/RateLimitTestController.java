package com.ratelimiter.ratelimiter;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimitTestController {

    private final TokenBucketRateLimiter limiter;

    public RateLimitTestController(TokenBucketRateLimiter limiter) {
        this.limiter = limiter;
    }

    @GetMapping("/api/limited")
    public ResponseEntity<Map<String, Object>> limited(
            @RequestHeader(value = "X-API-Key", defaultValue = "demo-key") String clientId) {
        RateLimitResult r = limiter.tryAcquire(clientId, 10, 1.0);
        if (!r.allowed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf((r.retryAfterMs() + 999) / 1000))
                    .body(Map.of("error", "Too Many Requests", "retryAfterMs", r.retryAfterMs()));
        }
        return ResponseEntity.ok(Map.of("message", "allowed", "remaining", r.remaining()));
    }
}