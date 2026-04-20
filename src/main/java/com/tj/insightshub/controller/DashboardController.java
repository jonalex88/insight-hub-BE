package com.tj.insightshub.controller;

import com.tj.insightshub.repository.DashboardRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardRepository repo;

    public DashboardController(DashboardRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/performers")
    public Map<String, Object> performers() {
        return repo.getPerformers();
    }
}
