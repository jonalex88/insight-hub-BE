package com.tj.insightshub.controller;

import com.tj.insightshub.repository.RuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleRepository repo;

    public RuleController(RuleRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return repo.findAll();
    }

    @GetMapping("/{entityId}")
    public List<Map<String, Object>> byEntity(@PathVariable String entityId) {
        return repo.findByEntity(entityId);
    }

    @PostMapping("/{entityId}")
    public ResponseEntity<?> create(@PathVariable String entityId,
                                    @RequestBody Map<String, Object> body) {
        String fieldType = (String) body.get("field_type");
        if (fieldType == null || fieldType.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "field_type required"));

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String[] txnTypes = toStringArray(body.get("transaction_types"));
        String operator = body.getOrDefault("operator", "eq").toString();
        String value = body.getOrDefault("value", "").toString();

        return ResponseEntity.status(201).body(
                repo.create(id, entityId, txnTypes, fieldType.trim(), operator, value));
    }

    @PutMapping("/{entityId}/{ruleId}")
    public ResponseEntity<?> update(@PathVariable String entityId,
                                    @PathVariable String ruleId,
                                    @RequestBody Map<String, Object> body) {
        String fieldType = (String) body.get("field_type");
        if (fieldType == null || fieldType.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "field_type required"));

        String[] txnTypes = toStringArray(body.get("transaction_types"));
        String operator = body.getOrDefault("operator", "eq").toString();
        String value = body.getOrDefault("value", "").toString();

        Optional<Map<String, Object>> result = repo.update(ruleId, entityId, txnTypes,
                fieldType.trim(), operator, value);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{entityId}/{ruleId}")
    public ResponseEntity<Void> delete(@PathVariable String entityId,
                                       @PathVariable String ruleId) {
        if (!repo.delete(ruleId, entityId)) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private static String[] toStringArray(Object value) {
        if (value == null) return new String[0];
        if (value instanceof List<?> list) return list.stream().map(Object::toString).toArray(String[]::new);
        return new String[0];
    }
}
