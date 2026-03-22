package com.ageoverflow.gcas.service;

import com.ageoverflow.gcas.dto.AnalysisRequestBody;
import com.ageoverflow.gcas.dto.AnalysisRequestDTO;
import com.ageoverflow.gcas.dto.GenerationsDTO;
import com.ageoverflow.gcas.dto.ResultDTO;
import com.ageoverflow.gcas.model.AnalysisRequest;
import com.ageoverflow.gcas.model.Customer;
import com.ageoverflow.gcas.model.RequestStatus;
import com.ageoverflow.gcas.repository.AnalysisRequestRepository;
import com.ageoverflow.gcas.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnalysisRequestService {

    private final CustomerRepository customerRepository;
    private final AnalysisRequestRepository analysisRequestRepository;
    private final RequestProcessorService requestProcessorService;

    public AnalysisRequestService(CustomerRepository customerRepository,
                                   AnalysisRequestRepository analysisRequestRepository,
                                   RequestProcessorService requestProcessorService) {
        this.customerRepository = customerRepository;
        this.analysisRequestRepository = analysisRequestRepository;
        this.requestProcessorService = requestProcessorService;
    }

    public AnalysisRequestDTO createRequest(UUID customerId, AnalysisRequestBody body) {
        Customer customer = customerRepository.findById(customerId)
                .orElseGet(() -> customerRepository.save(new Customer(customerId)));

        AnalysisRequest request = new AnalysisRequest();
        request.setId(UUID.randomUUID());
        request.setCustomer(customer);
        request.setUserId(body.getUserId());
        request.setUrgent(body.isUrgent());
        request.setPhotos(body.getPhotos());

        // Persist before kicking off processing — satisfies the persistence guarantee
        AnalysisRequest saved = analysisRequestRepository.save(request);

        requestProcessorService.processAsync(saved.getId());

        return toDTO(saved);
    }

    public List<AnalysisRequestDTO> getRequests(UUID customerId, int limit, int offset) {
        return analysisRequestRepository
                .findByCustomerId(customerId)
                .stream()
                .map(AnalysisRequestService::toDTO)
                .toList();
    }

    public static AnalysisRequestDTO toDTO(AnalysisRequest request) {
        ResultDTO result = null;
        if (request.getStatus() == RequestStatus.SUCCESS) {
            GenerationsDTO generations = new GenerationsDTO(
                    request.getGenSilent(),
                    request.getGenBabyBoomers(),
                    request.getGenX(),
                    request.getGenY(),
                    request.getGenZ(),
                    request.getGenAlpha()
            );
            result = new ResultDTO(
                    request.getChecksum(),
                    generations,
                    request.getPrimaryGeneration().getValue(),
                    request.getAge()
            );
        }

        return new AnalysisRequestDTO(
                request.getId(),
                request.getUserId(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getStatus(),
                result
        );
    }
}
