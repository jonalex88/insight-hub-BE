package com.tj.insightshub.controller;

import com.tj.insightshub.repository.ProjectionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/projections")
public class ProjectionController {

    private final ProjectionRepository repo;

    public ProjectionController(ProjectionRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll();
    }

    @GetMapping("/{entityId}")
    public ResponseEntity<?> get(@PathVariable String entityId) {
        Optional<Map<String, Object>> result = repo.findById(entityId);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{entityId}")
    public ResponseEntity<?> upsert(@PathVariable String entityId,
                                    @RequestBody Map<String, Object> body) {
        Object targetDate = body.get("target_date");
        if (targetDate == null)
            return ResponseEntity.badRequest().body(Map.of("error", "target_date is required"));
        return ResponseEntity.ok(repo.upsert(
                entityId,
                body.getOrDefault("start_date", null),
                targetDate.toString(),
                body.getOrDefault("lane_count", null),
                body.getOrDefault("txn_count", null),
                body.getOrDefault("monthly_value", null)
        ));
    }

    @DeleteMapping("/{entityId}")
    public ResponseEntity<Void> delete(@PathVariable String entityId) {
        repo.delete(entityId);
        return ResponseEntity.noContent().build();
    }
}
