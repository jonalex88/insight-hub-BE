package com.tj.insightshub.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PaymentMethodRepository {

    private final JdbcTemplate jdbc;

    public PaymentMethodRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll(String area) {
        return jdbc.queryForList(
                "SELECT id, name, short_name, area, tag, logo, description, sort_order" +
                " FROM payment_methods WHERE area = ? ORDER BY sort_order", area);
    }

    public List<Map<String, Object>> stats30d(String area) {
        return jdbc.queryForList("""
                WITH
                  current_window AS (
                    SELECT d.payment_method_id,
                      SUM(d.total_count)      AS total_count,
                      SUM(d.approved_count)   AS approved_count,
                      SUM(d.approved_value)   AS approved_value,
                      MAX(d.active_merchants) AS active_merchants
                    FROM daily_pm_stats d
                    JOIN payment_methods pm ON pm.id = d.payment_method_id AND pm.area = ?
                    WHERE d.date >= CURRENT_DATE - INTERVAL '30 days' AND d.date < CURRENT_DATE
                    GROUP BY d.payment_method_id
                  ),
                  prior_window AS (
                    SELECT d.payment_method_id,
                      MAX(d.active_merchants) AS active_merchants_prior
                    FROM daily_pm_stats d
                    JOIN payment_methods pm ON pm.id = d.payment_method_id AND pm.area = ?
                    WHERE d.date >= CURRENT_DATE - INTERVAL '60 days'
                      AND d.date < CURRENT_DATE - INTERVAL '30 days'
                    GROUP BY d.payment_method_id
                  )
                SELECT
                  pm.id, pm.name, pm.short_name, pm.area, pm.tag, pm.logo, pm.description,
                  COALESCE(c.total_count,    0) AS total_count,
                  COALESCE(c.approved_count, 0) AS approved_count,
                  COALESCE(c.approved_value, 0) AS approved_value,
                  COALESCE(c.active_merchants,0) AS active_merchants,
                  CASE WHEN COALESCE(c.total_count, 0) = 0 THEN NULL
                       ELSE ROUND(COALESCE(c.approved_count,0)::NUMERIC / c.total_count * 100, 2)
                  END AS approval_rate,
                  CASE WHEN COALESCE(p.active_merchants_prior, 0) = 0 THEN NULL
                       ELSE ROUND(
                         (COALESCE(c.active_merchants,0) - p.active_merchants_prior)::NUMERIC
                         / p.active_merchants_prior * 100, 2)
                  END AS merchant_growth_pct
                FROM payment_methods pm
                LEFT JOIN current_window c ON c.payment_method_id = pm.id
                LEFT JOIN prior_window   p ON p.payment_method_id = pm.id
                WHERE pm.area = ?
                ORDER BY COALESCE(c.active_merchants, 0) DESC, pm.sort_order
                """, area, area, area);
    }

    public List<Map<String, Object>> statsMonthly(String area, int months) {
        return jdbc.queryForList("""
                SELECT pm.id, pm.name, pm.short_name, pm.area, pm.tag, pm.logo,
                       s.year_month, s.total_count, s.approved_count, s.approved_value,
                       s.active_merchants, s.approval_rate, s.growth_pct
                FROM payment_methods pm
                JOIN monthly_pm_stats s ON s.payment_method_id = pm.id
                WHERE pm.area = ?
                  AND s.year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY pm.sort_order, s.year_month
                """, area, months);
    }

    public List<Map<String, Object>> statsDaily(String area, int days) {
        return jdbc.queryForList("""
                SELECT pm.id, d.date, d.total_count, d.approved_count, d.approved_value,
                       d.active_merchants, d.approval_rate
                FROM payment_methods pm
                JOIN daily_pm_stats d ON d.payment_method_id = pm.id
                WHERE pm.area = ?
                  AND d.date >= CURRENT_DATE - (? || ' days')::INTERVAL
                  AND d.date < CURRENT_DATE
                ORDER BY pm.sort_order, d.date
                """, area, days);
    }

    public List<Map<String, Object>> trend(String id, int months) {
        return jdbc.queryForList("""
                SELECT year_month, total_count, approved_count, approved_value,
                       active_merchants, approval_rate, growth_pct
                FROM monthly_pm_stats
                WHERE payment_method_id = ?
                  AND year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY year_month
                """, id, months);
    }

    public List<Map<String, Object>> daily(String id, int days) {
        return jdbc.queryForList("""
                SELECT date, total_count, approved_count, approved_value,
                       active_merchants, approval_rate
                FROM daily_pm_stats
                WHERE payment_method_id = ?
                  AND date >= CURRENT_DATE - (? || ' days')::INTERVAL
                ORDER BY date
                """, id, days);
    }

    public Map<String, Object> create(String id, String name, String shortName,
                                      String area, String tag, String logo, String description) {
        return jdbc.queryForMap("""
                INSERT INTO payment_methods (id, name, short_name, area, tag, logo, description, sort_order)
                VALUES (?, ?, ?, ?, ?, ?, ?, 9999)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
                RETURNING id, name, short_name, area, tag, logo, description, sort_order
                """, id, name, shortName, area, tag, logo, description);
    }

    public Optional<Map<String, Object>> patch(String id, String area,
                                               List<String> sets, List<Object> vals) {
        List<Object> params = new ArrayList<>(vals);
        params.add(id);
        params.add(area);
        String sql = "UPDATE payment_methods SET " + String.join(", ", sets) +
                " WHERE id = ? AND area = ? RETURNING id, name, short_name, description, logo";
        try {
            return Optional.of(jdbc.queryForMap(sql, params.toArray()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> getPerformers(String area) {
        return jdbc.queryForList("""
                SELECT DISTINCT ON (pm.id)
                  pm.id, pm.name, pm.short_name, pm.area, pm.tag,
                  COALESCE(s.active_merchants, 0) AS active_merchants,
                  s.growth_pct
                FROM payment_methods pm
                LEFT JOIN monthly_pm_stats s ON s.payment_method_id = pm.id
                WHERE pm.area = ?
                ORDER BY pm.id, s.year_month DESC NULLS LAST
                """, area);
    }
}
