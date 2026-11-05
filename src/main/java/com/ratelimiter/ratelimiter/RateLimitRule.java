package com.ratelimiter.ratelimiter;

public record RateLimitRule(int capacity, double refillPerSec) {}