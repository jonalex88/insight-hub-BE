package com.tj.insightshub.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RuleRepository {

    private final JdbcTemplate jdbc;

    public RuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Map<String, Object> mapRule(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", rs.getString("id"));
        rule.put("entity_id", rs.getString("entity_id"));
        Array arr = rs.getArray("transaction_types");
        rule.put("transaction_types", arr != null ? Arrays.asList((String[]) arr.getArray()) : List.of());
        rule.put("field_type", rs.getString("field_type"));
        rule.put("operator", rs.getString("operator"));
        rule.put("value", rs.getString("value"));
        return rule;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.query(
                "SELECT id, entity_id, transaction_types, field_type, operator, value" +
                " FROM rules ORDER BY entity_id, sort_order, created_at",
                this::mapRule);
    }

    public List<Map<String, Object>> findByEntity(String entityId) {
        return jdbc.query(
                "SELECT id, entity_id, transaction_types, field_type, operator, value" +
                " FROM rules WHERE entity_id = ? ORDER BY sort_order, created_at",
                this::mapRule, entityId);
    }

    public Map<String, Object> create(String id, String entityId, String[] txnTypes,
                                      String fieldType, String operator, String value) {
        return jdbc.execute((java.sql.Connection con) -> {
            String sql = """
                    INSERT INTO rules (id, entity_id, transaction_types, field_type, operator, value, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?,
                      (SELECT COALESCE(MAX(sort_order), 0) + 1 FROM rules WHERE entity_id = ?))
                    RETURNING id, entity_id, transaction_types, field_type, operator, value
                    """;
            try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, entityId);
                ps.setArray(3, con.createArrayOf("text", txnTypes));
                ps.setString(4, fieldType);
                ps.setString(5, operator);
                ps.setString(6, value);
                ps.setString(7, entityId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return mapRule(rs, 1);
                }
            }
        });
    }

    public Optional<Map<String, Object>> update(String ruleId, String entityId, String[] txnTypes,
                                                 String fieldType, String operator, String value) {
        try {
            return Optional.ofNullable(jdbc.execute((java.sql.Connection con) -> {
                String sql = """
                        UPDATE rules
                        SET transaction_types = ?, field_type = ?, operator = ?, value = ?
                        WHERE id = ? AND entity_id = ?
                        RETURNING id, entity_id, transaction_types, field_type, operator, value
                        """;
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setArray(1, con.createArrayOf("text", txnTypes));
                    ps.setString(2, fieldType);
                    ps.setString(3, operator);
                    ps.setString(4, value);
                    ps.setString(5, ruleId);
                    ps.setString(6, entityId);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) return null;
                        return mapRule(rs, 1);
                    }
                }
            }));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean delete(String ruleId, String entityId) {
        int rows = jdbc.update(
                "DELETE FROM rules WHERE id = ? AND entity_id = ?", ruleId, entityId);
        return rows > 0;
    }
}
