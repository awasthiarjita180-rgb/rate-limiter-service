package com.ratelimiter.ratelimiter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

class TokenBucketRateLimiterTest {

    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    static LettuceConnectionFactory factory;
    static TokenBucketRateLimiter limiter;

    @BeforeAll
    static void start() {
        redis.start();
        factory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        factory.afterPropertiesSet();
        limiter = new TokenBucketRateLimiter(new StringRedisTemplate(factory));
    }

    @AfterAll
    static void stop() {
        factory.destroy();
        redis.stop();
    }

    @Test
    void allowsUpToCapacityThenBlocks() {
        String client = "seq-" + UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryAcquire(client, 3, 0.001).allowed());
        }
        RateLimitResult blocked = limiter.tryAcquire(client, 3, 0.001);
        assertFalse(blocked.allowed());
        assertTrue(blocked.retryAfterMs() > 0);
    }

    @Test
    void clientsHaveSeparateBuckets() {
        String a = "a-" + UUID.randomUUID();
        String b = "b-" + UUID.randomUUID();
        assertTrue(limiter.tryAcquire(a, 1, 0.001).allowed());
        assertFalse(limiter.tryAcquire(a, 1, 0.001).allowed());
        assertTrue(limiter.tryAcquire(b, 1, 0.001).allowed());
    }

    @Test
    void tokensRefillOverTime() throws Exception {
        String client = "refill-" + UUID.randomUUID();
        assertTrue(limiter.tryAcquire(client, 1, 10).allowed());
        assertFalse(limiter.tryAcquire(client, 1, 10).allowed());
        Thread.sleep(300);
        assertTrue(limiter.tryAcquire(client, 1, 10).allowed());
    }

    @Test
    void neverAllowsMoreThanCapacityUnderConcurrency() throws Exception {
        String client = "conc-" + UUID.randomUUID();
        int capacity = 50;
        int requests = 400;
        ExecutorService pool = Executors.newFixedThreadPool(100);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                if (limiter.tryAcquire(client, capacity, 0.001).allowed()) {
                    allowed.incrementAndGet();
                }
                return null;
            }));
        }
        startGate.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(capacity, allowed.get());
    }
}