package com.ageoverflow.gcas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the health-check endpoint used by load balancers and readiness probes.
 */
@RestController
public class HealthController {

    /**
     * Returns 200 OK when the service is running and able to accept requests.
     * Used by Docker health checks and AWS load balancer target group checks.
     */
    @GetMapping("/api/v1/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}