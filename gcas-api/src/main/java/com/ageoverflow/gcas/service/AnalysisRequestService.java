package com.ageoverflow.gcas.service;

import com.ageoverflow.gcas.dto.AnalysisRequestBody;
import com.ageoverflow.gcas.dto.AnalysisRequestDTO;
import com.ageoverflow.gcas.dto.GenerationsDTO;
import com.ageoverflow.gcas.dto.ResultDTO;
import com.ageoverflow.gcas.model.AnalysisRequest;
import com.ageoverflow.gcas.model.Customer;
import com.ageoverflow.gcas.model.Generation;
import com.ageoverflow.gcas.model.RequestStatus;
import com.ageoverflow.gcas.repository.AnalysisRequestRepository;
import com.ageoverflow.gcas.repository.CustomerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ageoverflow.gcas.dto.UserDTO;
import com.ageoverflow.gcas.dto.UserResultDTO;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<AnalysisRequestDTO> getRequests(
            UUID customerId, int limit, int offset,
            OffsetDateTime start, OffsetDateTime end,
            UUID userId, RequestStatus status, Generation generation) {

        Specification<AnalysisRequest> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId));

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (start != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), start));
        }
        if (end != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), end));
        }
        if (generation != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("primaryGeneration"), generation));
        }

        return analysisRequestRepository
                .findAll(spec, Sort.by(Sort.Direction.ASC, "createdAt"))
                .stream()
                .skip(offset)
                .limit(limit)
                .map(AnalysisRequestService::toDTO)
                .toList();
    }

    public List<UserDTO> getUsers(UUID customerId, int limit, int offset) {
        Map<UUID, List<AnalysisRequest>> byUser = analysisRequestRepository
                .findByCustomerId(customerId)
                .stream()
                .collect(Collectors.groupingBy(AnalysisRequest::getUserId));

        return byUser.entrySet().stream()
                .map(e -> new UserDTO(e.getKey(), e.getValue().stream()
                        .map(AnalysisRequestService::toUserResultDTO)
                        .toList()))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    public Optional<UserDTO> getUserByCustomerAndUser(UUID customerId, UUID userId) {
        List<AnalysisRequest> requests = analysisRequestRepository
                .findByCustomerIdAndUserId(customerId, userId);
        if (requests.isEmpty()) return Optional.empty();
        return Optional.of(new UserDTO(userId, requests.stream()
                .map(AnalysisRequestService::toUserResultDTO)
                .toList()));
    }

    private static UserResultDTO toUserResultDTO(AnalysisRequest request) {
        String primaryGeneration = request.getPrimaryGeneration() != null
                ? request.getPrimaryGeneration().getValue() : null;
        return new UserResultDTO(
                request.getId(),
                request.getStatus().toLowerCase(),
                request.getChecksum(),
                primaryGeneration,
                request.getAge()
        );
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
