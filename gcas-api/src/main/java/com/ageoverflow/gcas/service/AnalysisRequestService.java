package com.ageoverflow.gcas.service;

import com.ageoverflow.gcas.dto.AnalysisRequestBody;
import com.ageoverflow.gcas.dto.AnalysisRequestDTO;
import com.ageoverflow.gcas.model.AnalysisRequest;
import com.ageoverflow.gcas.model.Customer;
import com.ageoverflow.gcas.repository.AnalysisRequestRepository;
import com.ageoverflow.gcas.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnalysisRequestService {

    private final CustomerRepository customerRepository;
    private final AnalysisRequestRepository analysisRequestRepository;

    public AnalysisRequestService(CustomerRepository customerRepository,
                                   AnalysisRequestRepository analysisRequestRepository) {
        this.customerRepository = customerRepository;
        this.analysisRequestRepository = analysisRequestRepository;
    }

    @Transactional
    public AnalysisRequestDTO createRequest(UUID customerId, AnalysisRequestBody body) {
        // Find the customer or create one if they don't exist yet
        Customer customer = customerRepository.findById(customerId)
                .orElseGet(() -> customerRepository.save(new Customer(customerId)));

        AnalysisRequest request = new AnalysisRequest();
        request.setId(UUID.randomUUID());
        request.setCustomer(customer);
        request.setUserId(body.getUserId());
        request.setUrgent(body.isUrgent());
        request.setPhotos(body.getPhotos());

        // Persist before returning
        AnalysisRequest saved = analysisRequestRepository.save(request);

        return toDTO(saved);
    }

    // Maps a saved AnalysisRequest entity to the API response shape.
    // Result is null here since the request is always pending at creation time.
    public static AnalysisRequestDTO toDTO(AnalysisRequest request) {
        return new AnalysisRequestDTO(
                request.getId(),
                request.getUserId(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getStatus(),
                null
        );
    }
}
