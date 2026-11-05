package com.ratelimiter.ratelimiter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final TokenBucketRateLimiter limiter;
    private final RuleService rules;

    public RateLimitFilter(TokenBucketRateLimiter limiter, RuleService rules) {
        this.limiter = limiter;
        this.rules = rules;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String clientId = req.getHeader("X-API-Key");
        if (clientId == null || clientId.isBlank()) {
            reject(res, 401, "Missing X-API-Key header");
            return;
        }
        Optional<RateLimitRule> rule = rules.find(clientId);
        if (rule.isEmpty()) {
            reject(res, 401, "Unknown API key");
            return;
        }

        RateLimitResult result;
        try {
            result = limiter.tryAcquire(clientId, rule.get().capacity(), rule.get().refillPerSec());
        } catch (Exception e) {
            log.error("Redis unavailable, failing open", e);
            chain.doFilter(req, res);
            return;
        }

        res.setHeader("X-RateLimit-Limit", String.valueOf(rule.get().capacity()));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));

        if (!result.allowed()) {
            res.setHeader("Retry-After", String.valueOf((result.retryAfterMs() + 999) / 1000));
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too Many Requests\",\"retryAfterMs\":" + result.retryAfterMs() + "}");
            return;
        }
        chain.doFilter(req, res);
    }

    private void reject(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}