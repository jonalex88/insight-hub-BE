package com.tj.insightshub.controller;

import com.tj.insightshub.repository.PosProviderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/pos-providers")
public class PosProviderController {

    private final PosProviderRepository repo;

    public PosProviderController(PosProviderRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Map<String, Object>> list() { return repo.findAll(); }

    @GetMapping("/stats/latest")
    public List<Map<String, Object>> statsLatest() { return repo.statsLatest(); }

    @GetMapping("/{id}/history")
    public List<Map<String, Object>> history(@PathVariable String id,
                                             @RequestParam(defaultValue = "90") int days) {
        return repo.statsHistory(id, Math.min(days, 365));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "name required"));
        String id = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String shortName       = body.getOrDefault("short_name", name).toString().trim();
        String logo            = body.containsKey("logo")             ? (String) body.get("logo")             : null;
        String description     = body.containsKey("description")      ? (String) body.get("description")      : null;
        String primaryIndustry = body.containsKey("primary_industry") ? (String) body.get("primary_industry") : null;
        return ResponseEntity.status(201).body(
                repo.create(id, name.trim(), shortName, logo, description, primaryIndustry));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> patch(@PathVariable String id, @RequestBody Map<String, Object> body) {
        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        if (body.containsKey("short_name"))       { Object v = body.get("short_name");       vals.add(v instanceof String s ? s.trim() : v); sets.add("short_name = ?"); }
        if (body.containsKey("description"))       { Object v = body.get("description");      vals.add(v instanceof String s ? s.trim() : v); sets.add("description = ?"); }
        if (body.containsKey("logo"))              { Object v = body.get("logo");             vals.add(v instanceof String s ? s.trim() : v); sets.add("logo = ?"); }
        if (body.containsKey("primary_industry"))  { Object v = body.get("primary_industry"); vals.add(v instanceof String s ? s.trim() : v); sets.add("primary_industry = ?"); }
        if (body.containsKey("rev_share_enabled")) { vals.add(body.get("rev_share_enabled")); sets.add("rev_share_enabled = ?"); }
        if (body.containsKey("rev_share_pct"))     { vals.add(body.get("rev_share_pct"));     sets.add("rev_share_pct = ?"); }
        if (sets.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Nothing to update"));
        Optional<Map<String, Object>> result = repo.patch(id, sets, vals);
        return result.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}
