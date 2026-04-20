package com.tj.insightshub.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class OverviewRepository {

    private final JdbcTemplate jdbc;

    public OverviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> getStats(String area) {
        if (area != null && !area.isBlank()) {
            return jdbc.queryForMap("""
                    SELECT
                      COALESCE(SUM(d.total_count),    0)  AS total_count,
                      COALESCE(SUM(d.approved_count), 0)  AS approved_count,
                      COALESCE(SUM(d.approved_value), 0)  AS approved_value,
                      COALESCE(MAX(d.active_merchants),0) AS active_merchants,
                      COUNT(DISTINCT d.payment_method_id) AS method_count
                    FROM daily_pm_stats d
                    JOIN payment_methods pm ON pm.id = d.payment_method_id
                    WHERE d.date >= CURRENT_DATE - INTERVAL '30 days'
                      AND d.date < CURRENT_DATE
                      AND pm.area = ?
                    """, area);
        }
        return jdbc.queryForMap("""
                SELECT
                  COALESCE(SUM(d.total_count),    0)  AS total_count,
                  COALESCE(SUM(d.approved_count), 0)  AS approved_count,
                  COALESCE(SUM(d.approved_value), 0)  AS approved_value,
                  COALESCE(MAX(d.active_merchants),0) AS active_merchants,
                  COUNT(DISTINCT d.payment_method_id) AS method_count
                FROM daily_pm_stats d
                JOIN payment_methods pm ON pm.id = d.payment_method_id
                WHERE d.date >= CURRENT_DATE - INTERVAL '30 days'
                  AND d.date < CURRENT_DATE
                """);
    }
}
