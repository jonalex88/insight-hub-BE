package com.tj.insightshub.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;
    private final PaymentMethodRepository pmRepo;
    private final SolutionTagRepository tagRepo;
    private final CountryRepository countryRepo;

    public DashboardRepository(JdbcTemplate jdbc,
                                PaymentMethodRepository pmRepo,
                                SolutionTagRepository tagRepo,
                                CountryRepository countryRepo) {
        this.jdbc = jdbc;
        this.pmRepo = pmRepo;
        this.tagRepo = tagRepo;
        this.countryRepo = countryRepo;
    }

    public Map<String, Object> getPerformers() {
        List<Map<String, Object>> pmRows      = pmRepo.getPerformers("Payment Methods");
        List<Map<String, Object>> solRows     = tagRepo.getPerformers();
        List<Map<String, Object>> platRows    = pmRepo.getPerformers("Platforms");
        List<Map<String, Object>> countryRows = countryRepo.getPerformers();

        List<Map<String, Object>> sections = List.of(
                buildSection("Payment Methods", pmRows),
                buildSection("Solutions",       solRows),
                buildSection("Platforms",       platRows),
                buildSection("Countries",       countryRows)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sections", sections);
        return result;
    }

    private Map<String, Object> buildSection(String area, List<Map<String, Object>> rows) {
        List<Map<String, Object>> sorted = new ArrayList<>(rows);
        sorted.sort(Comparator.comparing(
                r -> (BigDecimal) r.get("growth_pct"),
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        int topSize = Math.min(3, sorted.size());
        List<Map<String, Object>> top      = sorted.subList(0, topSize);
        List<Map<String, Object>> remaining = sorted.subList(topSize, sorted.size());
        List<Map<String, Object>> bottom   = remaining.subList(
                Math.max(0, remaining.size() - 3), remaining.size());

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("area",   area);
        section.put("top",    new ArrayList<>(top));
        section.put("bottom", new ArrayList<>(bottom));
        return section;
    }
}
