package com.tj.insightshub.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SolutionTagRepository {

    private final JdbcTemplate jdbc;

    public SolutionTagRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT id, name, short_name, area, tag, logo, description, sort_order" +
                " FROM solution_tags ORDER BY sort_order");
    }

    public List<Map<String, Object>> stats30d() {
        return jdbc.queryForList("""
                WITH
                  current_window AS (
                    SELECT solution_tag_id,
                      SUM(total_count)      AS total_count,
                      SUM(approved_count)   AS approved_count,
                      SUM(approved_value)   AS approved_value,
                      MAX(active_merchants) AS active_merchants
                    FROM daily_tag_stats
                    WHERE date >= CURRENT_DATE - INTERVAL '30 days' AND date < CURRENT_DATE
                    GROUP BY solution_tag_id
                  ),
                  prior_window AS (
                    SELECT solution_tag_id,
                      MAX(active_merchants) AS active_merchants_prior
                    FROM daily_tag_stats
                    WHERE date >= CURRENT_DATE - INTERVAL '60 days'
                      AND date < CURRENT_DATE - INTERVAL '30 days'
                    GROUP BY solution_tag_id
                  )
                SELECT
                  st.id, st.name, st.short_name, st.area, st.tag, st.logo, st.description,
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
                FROM solution_tags st
                LEFT JOIN current_window c ON c.solution_tag_id = st.id
                LEFT JOIN prior_window   p ON p.solution_tag_id = st.id
                ORDER BY COALESCE(c.active_merchants, 0) DESC, st.sort_order
                """);
    }

    public List<Map<String, Object>> statsMonthly(int months) {
        return jdbc.queryForList("""
                SELECT st.id, st.name, st.short_name, st.area, st.tag, st.logo,
                       s.year_month, s.total_count, s.approved_count, s.approved_value,
                       s.active_merchants, s.approval_rate, s.growth_pct
                FROM solution_tags st
                JOIN monthly_tag_stats s ON s.solution_tag_id = st.id
                WHERE s.year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY st.sort_order, s.year_month
                """, months);
    }

    public List<Map<String, Object>> statsDaily(int days) {
        return jdbc.queryForList("""
                SELECT st.id, d.date, d.total_count, d.approved_count, d.approved_value,
                       d.active_merchants, d.approval_rate
                FROM solution_tags st
                JOIN daily_tag_stats d ON d.solution_tag_id = st.id
                WHERE d.date >= CURRENT_DATE - (? || ' days')::INTERVAL
                  AND d.date < CURRENT_DATE
                ORDER BY st.sort_order, d.date
                """, days);
    }

    public List<Map<String, Object>> trend(String id, int months) {
        return jdbc.queryForList("""
                SELECT year_month, total_count, approved_count, approved_value,
                       active_merchants, approval_rate, growth_pct
                FROM monthly_tag_stats
                WHERE solution_tag_id = ?
                  AND year_month >= TO_CHAR(
                    (DATE_TRUNC('month', CURRENT_DATE) - (? - 1 || ' months')::INTERVAL), 'YYYY-MM')
                ORDER BY year_month
                """, id, months);
    }

    public List<Map<String, Object>> daily(String id, int days) {
        return jdbc.queryForList("""
                SELECT date, total_count, approved_count, approved_value,
                       active_merchants, approval_rate
                FROM daily_tag_stats
                WHERE solution_tag_id = ?
                  AND date >= CURRENT_DATE - (? || ' days')::INTERVAL
                ORDER BY date
                """, id, days);
    }

    public Map<String, Object> create(String id, String name, String shortName,
                                      String tag, String logo, String description) {
        return jdbc.queryForMap("""
                INSERT INTO solution_tags (id, name, short_name, area, tag, logo, description, sort_order)
                VALUES (?, ?, ?, 'Solutions', ?, ?, ?, 9999)
                ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
                RETURNING id, name, short_name, area, tag, logo, description, sort_order
                """, id, name, shortName, tag, logo, description);
    }

    public Optional<Map<String, Object>> patch(String id, List<String> sets, List<Object> vals) {
        List<Object> params = new ArrayList<>(vals);
        params.add(id);
        String sql = "UPDATE solution_tags SET " + String.join(", ", sets) +
                " WHERE id = ? RETURNING id, name, short_name, description, logo";
        try {
            return Optional.of(jdbc.queryForMap(sql, params.toArray()));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Map<String, Object>> getPerformers() {
        return jdbc.queryForList("""
                SELECT DISTINCT ON (st.id)
                  st.id, st.name, st.short_name, st.area, st.tag,
                  COALESCE(s.active_merchants, 0) AS active_merchants,
                  s.growth_pct
                FROM solution_tags st
                LEFT JOIN monthly_tag_stats s ON s.solution_tag_id = st.id
                ORDER BY st.id, s.year_month DESC NULLS LAST
                """);
    }
}
