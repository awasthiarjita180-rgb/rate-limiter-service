package com.ratelimiter.ratelimiter;

import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redis;
    private final DefaultRedisScript<List> script = new DefaultRedisScript<>();

    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/token_bucket.lua")));
        script.setResultType(List.class);
    }

    public RateLimitResult tryAcquire(String clientId, int capacity, double refillPerSec) {
        List<?> r = redis.execute(script, List.of("rate:" + clientId),
                String.valueOf(capacity), String.valueOf(refillPerSec), "1");
        return new RateLimitResult(
                ((Number) r.get(0)).longValue() == 1,
                ((Number) r.get(1)).longValue(),
                ((Number) r.get(2)).longValue());
    }
}