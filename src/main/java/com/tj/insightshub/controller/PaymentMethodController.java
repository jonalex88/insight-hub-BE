package com.tj.insightshub.controller;

import com.tj.insightshub.repository.PaymentMethodRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private static final String AREA = "Payment Methods";
    private final PaymentMethodRepository repo;

    public PaymentMethodController(PaymentMethodRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll(AREA);
    }

    @GetMapping("/stats/30d")
    public List<Map<String, Object>> stats30d() {
        return repo.stats30d(AREA);
    }

    @GetMapping("/stats/monthly")
    public List<Map<String, Object>> statsMonthly(@RequestParam(defaultValue = "12") int months) {
        return repo.statsMonthly(AREA, Math.min(months, 36));
    }

    @GetMapping("/stats/daily")
    public List<Map<String, Object>> statsDaily(@RequestParam(defaultValue = "30") int days) {
        return repo.statsDaily(AREA, Math.min(days, 30));
    }

    @GetMapping("/{id}/trend")
    public List<Map<String, Object>> trend(@PathVariable String id,
                                           @RequestParam(defaultValue = "12") int months) {
        return repo.trend(id, Math.min(months, 36));
    }

    @GetMapping("/{id}/daily")
    public List<Map<String, Object>> daily(@PathVariable String id,
                                           @RequestParam(defaultValue = "30") int days) {
        return repo.daily(id, Math.min(days, 30));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "name required"));
        String id = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String shortName = body.getOrDefault("short_name", name).toString().trim();
        String tag = body.containsKey("tag") ? (String) body.get("tag") : null;
        String logo = body.containsKey("logo") ? (String) body.get("logo") : null;
        String description = body.containsKey("description") ? (String) body.get("description") : null;
        return ResponseEntity.status(201).body(
                repo.create(id, name.trim(), shortName, AREA, tag, logo, description));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable String id, @RequestBody Map<String, Object> body) {
        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        addPatchField("short_name", body, sets, vals);
        addPatchField("description", body, sets, vals);
        addPatchField("logo", body, sets, vals);
        if (sets.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Nothing to update"));
        Optional<Map<String, Object>> result = repo.patch(id, AREA, sets, vals);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private static void addPatchField(String key, Map<String, Object> body,
                                      List<String> sets, List<Object> vals) {
        if (!body.containsKey(key)) return;
        Object v = body.get(key);
        vals.add(v instanceof String s ? s.trim() : v);
        sets.add(key + " = ?");
    }
}
