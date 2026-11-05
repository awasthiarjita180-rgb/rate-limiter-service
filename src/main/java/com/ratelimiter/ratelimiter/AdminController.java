package com.ratelimiter.ratelimiter;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    private final JdbcTemplate jdbc;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/admin/blocked")
    public List<Map<String, Object>> blocked(@RequestParam(defaultValue = "50") int limit) {
        return jdbc.queryForList(
                "SELECT client_id, path, blocked_at FROM blocked_requests ORDER BY blocked_at DESC LIMIT ?",
                Math.min(Math.max(limit, 1), 200));
    }
}