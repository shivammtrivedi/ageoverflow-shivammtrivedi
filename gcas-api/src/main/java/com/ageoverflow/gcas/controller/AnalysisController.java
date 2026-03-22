package com.ageoverflow.gcas.controller;

import com.ageoverflow.gcas.dto.AnalysisRequestBody;
import com.ageoverflow.gcas.dto.AnalysisRequestDTO;
import com.ageoverflow.gcas.model.AnalysisRequest;
import com.ageoverflow.gcas.model.Generation;
import com.ageoverflow.gcas.model.RequestStatus;
import com.ageoverflow.gcas.repository.AnalysisRequestRepository;
import com.ageoverflow.gcas.service.AnalysisRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String user_id,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String generation) {

        UUID customerUUID;
        try {
            customerUUID = UUID.fromString(customerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid customer_id: must be a valid UUID");
        }

        if (limit < 1 || limit > 1000) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("limit must be between 1 and 1000");
        }

        if (offset < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("offset must be >= 0");
        }

        OffsetDateTime startDT = null;
        if (start != null) {
            try {
                startDT = OffsetDateTime.parse(start);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid start: must be RFC3339 date-time");
            }
        }

        OffsetDateTime endDT = null;
        if (end != null) {
            try {
                endDT = OffsetDateTime.parse(end);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid end: must be RFC3339 date-time");
            }
        }

        UUID userUUID = null;
        if (user_id != null) {
            try {
                userUUID = UUID.fromString(user_id);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid user_id: must be a valid UUID");
            }
        }

        RequestStatus statusEnum = null;
        if (status != null) {
            try {
                statusEnum = RequestStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid status: must be pending, success, or failed");
            }
        }

        Generation generationEnum = null;
        if (generation != null) {
            try {
                generationEnum = Generation.fromValue(generation);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid generation: must be silent, baby_boomers, x, y, z, or alpha");
            }
        }

        return ResponseEntity.ok(analysisRequestService.getRequests(
                customerUUID, limit, offset, startDT, endDT, userUUID, statusEnum, generationEnum));
    }

    @GetMapping("/{customer_id}/users")
    public ResponseEntity<?> getUsers(
            @PathVariable("customer_id") String customerId,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        UUID customerUUID;
        try {
            customerUUID = UUID.fromString(customerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid customer_id: must be a valid UUID");
        }

        if (limit < 1 || limit > 1000) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("limit must be between 1 and 1000");
        }

        if (offset < 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("offset must be >= 0");
        }

        return ResponseEntity.ok(analysisRequestService.getUsers(customerUUID, limit, offset));
    }

    @GetMapping("/{customer_id}/users/{user_id}")
    public ResponseEntity<?> getUser(
            @PathVariable("customer_id") String customerId,
            @PathVariable("user_id") String userId) {

        UUID customerUUID;
        try {
            customerUUID = UUID.fromString(customerId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid customer_id: must be a valid UUID");
        }

        UUID userUUID;
        try {
            userUUID = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid user_id: must be a valid UUID");
        }

        return analysisRequestService.getUserByCustomerAndUser(customerUUID, userUUID)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
