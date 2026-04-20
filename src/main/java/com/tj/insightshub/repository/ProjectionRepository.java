package com.tj.insightshub.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ProjectionRepository {

    private final JdbcTemplate jdbc;

    public ProjectionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLS =
            "entity_id, start_date, target_date, lane_count, txn_count, monthly_value, updated_at";

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT " + SELECT_COLS + " FROM projections ORDER BY updated_at DESC");
    }

    public Optional<Map<String, Object>> findById(String entityId) {
        try {
            return Optional.of(jdbc.queryForMap(
                    "SELECT " + SELECT_COLS + " FROM projections WHERE entity_id = ?", entityId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Map<String, Object> upsert(String entityId, Object startDate, String targetDate,
                                      Object laneCount, Object txnCount, Object monthlyValue) {
        return jdbc.queryForMap("""
                INSERT INTO projections (entity_id, start_date, target_date, lane_count, txn_count, monthly_value, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (entity_id) DO UPDATE SET
                  start_date    = EXCLUDED.start_date,
                  target_date   = EXCLUDED.target_date,
                  lane_count    = EXCLUDED.lane_count,
                  txn_count     = EXCLUDED.txn_count,
                  monthly_value = EXCLUDED.monthly_value,
                  updated_at    = NOW()
                RETURNING entity_id, start_date, target_date, lane_count, txn_count, monthly_value, updated_at
                """, entityId, startDate, targetDate, laneCount, txnCount, monthlyValue);
    }

    public void delete(String entityId) {
        jdbc.update("DELETE FROM projections WHERE entity_id = ?", entityId);
    }
}
