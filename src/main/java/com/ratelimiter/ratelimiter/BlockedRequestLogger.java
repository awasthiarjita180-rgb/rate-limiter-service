package com.ratelimiter.ratelimiter;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BlockedRequestLogger {

    private static final Logger log = LoggerFactory.getLogger(BlockedRequestLogger.class);

    private final JdbcTemplate jdbc;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public BlockedRequestLogger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String clientId, String path) {
        executor.submit(() -> {
            try {
                jdbc.update("INSERT INTO blocked_requests (client_id, path) VALUES (?, ?)", clientId, path);
            } catch (Exception e) {
                log.warn("Could not record blocked request for {}", clientId, e);
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}