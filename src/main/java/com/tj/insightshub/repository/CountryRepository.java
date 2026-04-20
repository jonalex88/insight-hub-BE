package com.tj.insightshub.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class CountryRepository {

    private final JdbcTemplate jdbc;

    public CountryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT id, name, short_name, logo, description, sort_order" +
                " FROM countries ORDER BY sort_order");
    }

    public List<Map<String, Object>> stats30d() {
        return jdbc.queryForList("""
                WITH
                  current_window AS (
                    SELECT country_id,
                      SUM(total_count)      AS total_count,
                      SUM(approved_count)   AS approved_count,
                      SUM(approved_value)   AS approved_value,
                      MAX(active_merchants) AS active_merchants
                    FROM daily_country_stats
                    WHERE date >= CURRENT_DATE - INTERVAL '30 days' AND date < CURRENT_DATE
                    GROUP BY country_id
                  ),
                  prior_window AS (
                    SELECT country_id,
                      MAX(active_merchants) AS active_merchants_prior
                    FROM daily_country_stats
                    WHERE date >= CURRENT_DATE - INTERVAL '60 days'
                      AND date < CURRENT_DATE - INTERVAL '30 days'
                    GROUP BY country_id
                  )
                SELECT
                  c.id, c.name, c.short_name, c.logo, c.description,
                  COALESCE(cw.total_count,    0) AS total_count,
                  COALESCE(cw.approved_count, 0) AS approved_count,
                  COALESCE(cw.approved_value, 0) AS approved_value,
                  COALESCE(cw.active_merchants,0) AS active_merchants,
                  CASE WHEN COALESCE(cw.total_count, 0) = 0 THEN NULL
                       ELSE ROUND(COALESCE(cw.approved_count,0)::NUMERIC / cw.total_count * 100, 2)
                  END AS approval_rate,
                  CASE WHEN COALESCE(pw.active_merchants_prior, 0) = 0 THEN NULL
                       ELSE ROUND(
                         (COALESCE(cw.active_merchants,0) - pw.active_merchants_prior)::NUMERIC
                         / pw.active_merchants_prior * 100, 2)
                  END AS merchant_growth_pct
                FROM countries c
                LEFT JOIN current_window cw ON cw.country_id = c.id
                LEFT JOIN prior_window   pw ON pw.country_id = c.id
                ORDER BY COALESCE(cw.active_merchants, 0) DESC, c.sort_order
                """);
    }

    public List<Map<String, Object>> statsMonthly(int months) {
        return jdbc.queryForList("""
                SELECT c.id, c.name, c.short_name, c.logo,
                       s.year_month, s.total_count, s.approved_count, s.approved_value,
                       s.active_merchants, s.approval_rate, s.growth_pct
                FROM countries c
                JOIN monthly_country_stats s ON s.country_id = c.id
                WHERE s.year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY c.sort_order, s.year_month
                """, months);
    }

    public List<Map<String, Object>> statsDaily(int days) {
        return jdbc.queryForList("""
                SELECT c.id, d.date, d.total_count, d.approved_count, d.approved_value,
                       d.active_merchants, d.approval_rate
                FROM countries c
                JOIN daily_country_stats d ON d.country_id = c.id
                WHERE d.date >= CURRENT_DATE - (? || ' days')::INTERVAL
                  AND d.date < CURRENT_DATE
                ORDER BY c.sort_order, d.date
                """, days);
    }

    public List<Map<String, Object>> trend(String id, int months) {
        return jdbc.queryForList("""
                SELECT year_month, total_count, approved_count, approved_value,
                       active_merchants, approval_rate, growth_pct
                FROM monthly_country_stats
                WHERE country_id = ?
                  AND year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY year_month
                """, id, months);
    }

    public List<Map<String, Object>> daily(String id, int days) {
        return jdbc.queryForList("""
                SELECT date, total_count, approved_count, approved_value,
                       active_merchants, approval_rate
                FROM daily_country_stats
                WHERE country_id = ?
                  AND date >= CURRENT_DATE - (? || ' days')::INTERVAL
                ORDER BY date
                """, id, days);
    }

    public Map<String, Object> create(String id, String name, String shortName,
                                      String logo, String description) {
        return jdbc.queryForMap("""
                INSERT INTO countries (id, name, short_name, logo, description, sort_order)
                VALUES (?, ?, ?, ?, ?, 9999)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
                RETURNING id, name, short_name, logo, description, sort_order
                """, id, name, shortName, logo, description);
    }

    public Optional<Map<String, Object>> patch(String id, List<String> sets, List<Object> vals) {
        List<Object> params = new ArrayList<>(vals);
        params.add(id);
        String sql = "UPDATE countries SET " + String.join(", ", sets) +
                " WHERE id = ? RETURNING id, name, short_name, description, logo";
        try {
            return Optional.of(jdbc.queryForMap(sql, params.toArray()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> getPerformers() {
        return jdbc.queryForList("""
                SELECT DISTINCT ON (c.id)
                  c.id, c.name, c.name AS short_name, 'Countries' AS area, NULL AS tag,
                  COALESCE(s.active_merchants, 0) AS active_merchants,
                  s.growth_pct
                FROM countries c
                LEFT JOIN monthly_country_stats s ON s.country_id = c.id
                ORDER BY c.id, s.year_month DESC NULLS LAST
                """);
    }
}
