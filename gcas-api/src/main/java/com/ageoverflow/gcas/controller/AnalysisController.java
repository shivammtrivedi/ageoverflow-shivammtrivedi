package com.ageoverflow.gcas.controller;

import com.ageoverflow.gcas.dto.AnalysisRequestBody;
import com.ageoverflow.gcas.dto.AnalysisRequestDTO;
import com.ageoverflow.gcas.model.AnalysisRequest;
import com.ageoverflow.gcas.repository.AnalysisRequestRepository;
import com.ageoverflow.gcas.service.AnalysisRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final AnalysisRequestService analysisRequestService;
    private final AnalysisRequestRepository analysisRequestRepository;

    public AnalysisController(AnalysisRequestService analysisRequestService,
                               AnalysisRequestRepository analysisRequestRepository) {
        this.analysisRequestService = analysisRequestService;
        this.analysisRequestRepository = analysisRequestRepository;
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

        // Validate photos is not empty
        if (body.getPhotos() == null || body.getPhotos().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("photos must not be empty");
        }

        AnalysisRequestDTO response = analysisRequestService.createRequest(customerUUID, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{customer_id}/requests/{request_id}")
    public ResponseEntity<?> getRequest(
            @PathVariable("customer_id") String customerId,
            @PathVariable("request_id") String requestId) {

        UUID customerUUID;
        try {
            customerUUID = UUID.fromString(customerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid customer_id: must be a valid UUID");
        }

        UUID requestUUID;
        try {
            requestUUID = UUID.fromString(requestId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request_id: must be a valid UUID");
        }

        AnalysisRequest request = analysisRequestRepository.findById(requestUUID).orElse(null);
        if (request == null || !request.getCustomer().getId().equals(customerUUID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Request not found");
        }

        return ResponseEntity.ok(AnalysisRequestService.toDTO(request));
    }

    @GetMapping("/{customer_id}/requests")
    public ResponseEntity<?> getRequests(
            @PathVariable("customer_id") String customerId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        UUID customerUUID;
        try {
            customerUUID = UUID.fromString(customerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid customer_id: must be a valid UUID");
        }
        return ResponseEntity.ok(analysisRequestService.getRequests(customerUUID, limit, offset));
    }
}
