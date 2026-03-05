package com.ageoverflow.gcas.controller;

import com.ageoverflow.gcas.dto.AnalysisRequestBody;
import com.ageoverflow.gcas.dto.AnalysisRequestDTO;
import com.ageoverflow.gcas.service.AnalysisRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisRequestService analysisRequestService;

    public AnalysisController(AnalysisRequestService analysisRequestService) {
        this.analysisRequestService = analysisRequestService;
    }

    @PostMapping("/{customer_id}/requests")
    public ResponseEntity<?> createRequest(
            @PathVariable("customer_id") String customerId,
            @RequestBody(required = false) AnalysisRequestBody body) {

        // Validate customer_id is a valid UUID
        UUID customerUUID;
        try {
            customerUUID = UUID.fromString(customerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid customer_id: must be a valid UUID");
        }

        // Validate body is present
        if (body == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Request body is required");
        }

        // Validate user_id is present
        if (body.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("user_id is required");
        }

        AnalysisRequestDTO response = analysisRequestService.createRequest(customerUUID, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
