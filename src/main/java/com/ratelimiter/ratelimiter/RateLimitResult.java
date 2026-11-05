package com.ratelimiter.ratelimiter;

public record RateLimitResult(boolean allowed, long remaining, long retryAfterMs) {}