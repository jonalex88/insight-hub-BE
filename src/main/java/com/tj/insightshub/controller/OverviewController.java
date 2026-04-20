package com.tj.insightshub.controller;

import com.tj.insightshub.repository.OverviewRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/overview")
public class OverviewController {

    private final OverviewRepository repo;

    public OverviewController(OverviewRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats(@RequestParam(required = false) String area) {
        return repo.getStats(area);
    }
}
