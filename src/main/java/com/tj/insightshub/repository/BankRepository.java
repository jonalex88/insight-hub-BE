package com.tj.insightshub.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BankRepository {

    private final JdbcTemplate jdbc;

    public BankRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT id, name, short_name, tag, logo, description, sort_order" +
                " FROM banks ORDER BY sort_order");
    }

    public List<Map<String, Object>> stats30d() {
        return jdbc.queryForList("""
                WITH
                  current_window AS (
                    SELECT bank_id,
                      SUM(total_count)      AS total_count,
                      SUM(approved_count)   AS approved_count,
                      SUM(approved_value)   AS approved_value,
                      MAX(active_merchants) AS active_merchants
                    FROM daily_bank_stats
                    WHERE date >= CURRENT_DATE - INTERVAL '30 days' AND date < CURRENT_DATE
                    GROUP BY bank_id
                  ),
                  prior_window AS (
                    SELECT bank_id,
                      MAX(active_merchants) AS active_merchants_prior
                    FROM daily_bank_stats
                    WHERE date >= CURRENT_DATE - INTERVAL '60 days'
                      AND date < CURRENT_DATE - INTERVAL '30 days'
                    GROUP BY bank_id
                  )
                SELECT
                  b.id, b.name, b.short_name, b.tag, b.logo, b.description, b.region,
                  COALESCE(cw.total_count,     0) AS total_count,
                  COALESCE(cw.approved_count,  0) AS approved_count,
                  COALESCE(cw.approved_value,  0) AS approved_value,
                  COALESCE(cw.active_merchants,0) AS active_merchants,
                  CASE WHEN COALESCE(cw.total_count, 0) = 0 THEN NULL
                       ELSE ROUND(COALESCE(cw.approved_count,0)::NUMERIC / cw.total_count * 100, 2)
                  END AS approval_rate,
                  CASE WHEN COALESCE(pw.active_merchants_prior, 0) = 0 THEN NULL
                       ELSE ROUND(
                         (COALESCE(cw.active_merchants,0) - pw.active_merchants_prior)::NUMERIC
                         / pw.active_merchants_prior * 100, 2)
                  END AS merchant_growth_pct
                FROM banks b
                LEFT JOIN current_window cw ON cw.bank_id = b.id
                LEFT JOIN prior_window   pw ON pw.bank_id = b.id
                ORDER BY COALESCE(cw.active_merchants, 0) DESC, b.sort_order
                """);
    }

    public List<Map<String, Object>> statsMonthly(int months) {
        return jdbc.queryForList("""
                SELECT b.id, b.name, b.short_name, b.tag, b.logo,
                       s.year_month, s.total_count, s.approved_count, s.approved_value,
                       s.active_merchants, s.approval_rate, s.growth_pct
                FROM banks b
                JOIN monthly_bank_stats s ON s.bank_id = b.id
                WHERE s.year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY b.sort_order, s.year_month
                """, months);
    }

    public List<Map<String, Object>> statsDaily(int days) {
        return jdbc.queryForList("""
                SELECT b.id, d.date, d.total_count, d.approved_count, d.approved_value,
                       d.active_merchants, d.approval_rate
                FROM banks b
                JOIN daily_bank_stats d ON d.bank_id = b.id
                WHERE d.date >= CURRENT_DATE - (? || ' days')::INTERVAL
                  AND d.date < CURRENT_DATE
                ORDER BY b.sort_order, d.date
                """, days);
    }

    public List<Map<String, Object>> trend(String id, int months) {
        return jdbc.queryForList("""
                SELECT year_month, total_count, approved_count, approved_value,
                       active_merchants, approval_rate, growth_pct
                FROM monthly_bank_stats
                WHERE bank_id = ?
                  AND year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY year_month
                """, id, months);
    }

    public List<Map<String, Object>> daily(String id, int days) {
        return jdbc.queryForList("""
                SELECT date, total_count, approved_count, approved_value,
                       active_merchants, approval_rate
                FROM daily_bank_stats
                WHERE bank_id = ?
                  AND date >= CURRENT_DATE - (? || ' days')::INTERVAL
                ORDER BY date
                """, id, days);
    }

    public Map<String, Object> create(String id, String name, String shortName,
                                      String tag, String logo, String description) {
        return jdbc.queryForMap("""
                INSERT INTO banks (id, name, short_name, tag, logo, description, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, 9999)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
                RETURNING id, name, short_name, tag, logo, description, sort_order
                """, id, name, shortName, tag, logo, description);
    }

    public Optional<Map<String, Object>> patch(String id, List<String> sets, List<Object> vals) {
        List<Object> params = new ArrayList<>(vals);
        params.add(id);
        String sql = "UPDATE banks SET " + String.join(", ", sets) +
                " WHERE id = ? RETURNING id, name, short_name, description, logo";
        try {
            return Optional.of(jdbc.queryForMap(sql, params.toArray()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
