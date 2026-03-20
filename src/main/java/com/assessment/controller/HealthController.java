package com.assessment.controller;

import com.assessment.model.SBApiResponse;
import com.assessment.service.HealthCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final HealthCheckService healthCheckService;

    public HealthController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping
    public ResponseEntity<SBApiResponse> health() {
        SBApiResponse response = healthCheckService.checkHealth();
        return new ResponseEntity<>(response, response.getResponseCode());
    }

    @GetMapping("/live")
    public ResponseEntity<?> liveness() {
        return ResponseEntity.ok(
                java.util.Map.of("status", "UP", "message", "Service is alive")
        );
    }
}