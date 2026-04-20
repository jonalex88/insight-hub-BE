package com.tj.insightshub.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PosProviderRepository {

    private final JdbcTemplate jdbc;

    public PosProviderRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT id, name, short_name, logo, description, primary_industry," +
                " rev_share_enabled, rev_share_pct, sort_order" +
                " FROM pos_providers ORDER BY sort_order");
    }

    public List<Map<String, Object>> statsLatest() {
        return jdbc.queryForList("""
                SELECT p.id, p.name, p.short_name, p.logo, p.description,
                       p.primary_industry, p.rev_share_enabled, p.rev_share_pct,
                       COALESCE(s.store_count, 0) AS store_count,
                       COALESCE(s.lane_count,  0) AS lane_count
                FROM pos_providers p
                LEFT JOIN LATERAL (
                    SELECT store_count, lane_count
                    FROM pos_provider_stats
                    WHERE pos_provider_id = p.id
                    ORDER BY date DESC
                    LIMIT 1
                ) s ON TRUE
                ORDER BY p.sort_order
                """);
    }

    public List<Map<String, Object>> statsHistory(String id, int days) {
        return jdbc.queryForList("""
                SELECT date, store_count, lane_count
                FROM pos_provider_stats
                WHERE pos_provider_id = ?
                  AND date >= CURRENT_DATE - (? || ' days')::INTERVAL
                ORDER BY date
                """, id, days);
    }

    public Map<String, Object> create(String id, String name, String shortName,
                                      String logo, String description, String primaryIndustry) {
        return jdbc.queryForMap("""
                INSERT INTO pos_providers (id, name, short_name, logo, description, primary_industry, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, 9999)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
                RETURNING id, name, short_name, logo, description, primary_industry,
                          rev_share_enabled, rev_share_pct, sort_order
                """, id, name, shortName, logo, description, primaryIndustry);
    }

    public Optional<Map<String, Object>> patch(String id, List<String> sets, List<Object> vals) {
        List<Object> params = new ArrayList<>(vals);
        params.add(id);
        String sql = "UPDATE pos_providers SET " + String.join(", ", sets) +
                " WHERE id = ? RETURNING id, name, short_name, description, logo," +
                " primary_industry, rev_share_enabled, rev_share_pct";
        try {
            return Optional.of(jdbc.queryForMap(sql, params.toArray()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
