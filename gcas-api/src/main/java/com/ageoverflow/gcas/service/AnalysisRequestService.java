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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ageoverflow.gcas.dto.StatisticsContentsDTO;
import com.ageoverflow.gcas.dto.StatisticsDTO;
import com.ageoverflow.gcas.dto.UserDTO;
import com.ageoverflow.gcas.dto.UserResultDTO;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
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
    private final SqsService sqsService;
    private final String urgentQueueUrl;

    public AnalysisRequestService(CustomerRepository customerRepository,
                                   AnalysisRequestRepository analysisRequestRepository,
                                   RequestProcessorService requestProcessorService,
                                   SqsService sqsService,
                                   @Value("${aws.sqs.urgent-queue-url}") String urgentQueueUrl) {
        this.customerRepository = customerRepository;
        this.analysisRequestRepository = analysisRequestRepository;
        this.requestProcessorService = requestProcessorService;
        this.sqsService = sqsService;
        this.urgentQueueUrl = urgentQueueUrl;
    }

    /**
     * Creates and persists a new analysis request, then kicks off async engine processing.
     * The customer account is created automatically if it does not yet exist.
     * Persistence is guaranteed before the method returns
     * that data is saved before a success response is sent to the caller.
     */
    @org.springframework.transaction.annotation.Transactional
    public AnalysisRequestDTO createRequest(UUID customerId, AnalysisRequestBody body) {
        // Atomic upsert: concurrent first-requests for the same customer_id are safe.
        // ON CONFLICT DO NOTHING means only one thread inserts; the rest skip silently.
        customerRepository.insertIfAbsent(customerId);
        Customer customer = customerRepository.findById(customerId).orElseThrow();

        AnalysisRequest request = new AnalysisRequest();
        request.setId(UUID.randomUUID());
        request.setCustomer(customer);
        request.setUserId(body.getUserId());
        request.setUrgent(body.isUrgent());
        request.setPhotos(body.getPhotos());

        // Persist before kicking off processing, satisfies the persistence guarantee
        AnalysisRequest saved = analysisRequestRepository.save(request);

        // Schedule processing AFTER this transaction commits so the row is visible.
        // If SQS is configured (queue URL non-empty), enqueue to SQS so the worker picks it up.
        // Otherwise fall back to in-process async (local dev without SQS).
        UUID requestId = saved.getId();
        boolean urgent = saved.isUrgent();
        org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        if (urgentQueueUrl != null && !urgentQueueUrl.isBlank()) {
                            sqsService.enqueue(requestId, urgent);
                        } else {
                            requestProcessorService.processAsync(requestId);
                        }
                    }
                });

        return toDTO(saved);
    }

    /**
     * Returns a filtered, paginated list of analysis requests for a customer.
     * All filter parameters are optional and additive; only supplied filters are applied.
     * Results are ordered by creation time ascending before limit/offset are applied.
     */
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

        // Push offset/limit to the database rather than fetching all rows and discarding in Java.
        // PageRequest.of(page, size) only works when offset is a multiple of limit, so we use a
        // custom Pageable that passes offset directly as the SQL OFFSET value.
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable pageable = new OffsetPageable(offset, limit, sort);
        return analysisRequestRepository
                .findAll(spec, pageable)
                .getContent()
                .stream()
                .map(AnalysisRequestService::toDTO)
                .toList();
    }

    /**
     * Returns a paginated list of all users associated with a customer.
     * Two-query approach, first fetch the paginated user IDs from the DB (tiny),
     * then fetch only the requests for those users. Avoids loading every request in memory.
     * Users are ordered by user_id for stable pagination across calls.
     */
    public List<UserDTO> getUsers(UUID customerId, int limit, int offset) {
        // Query 1: DB-side DISTINCT user_id with LIMIT/OFFSET — returns only the IDs we need
        Pageable pageable = new OffsetPageable(offset, limit, Sort.unsorted());
        List<UUID> userIds = analysisRequestRepository
                .findDistinctUserIdsByCustomerId(customerId, pageable);

        if (userIds.isEmpty()) {
            return List.of();
        }

        // Query 2: fetch requests only for the users on this page
        Map<UUID, List<AnalysisRequest>> byUser = analysisRequestRepository
                .findByCustomerIdAndUserIdIn(customerId, userIds)
                .stream()
                .collect(Collectors.groupingBy(AnalysisRequest::getUserId));

        return userIds.stream()
                .map(uid -> new UserDTO(uid, byUser.getOrDefault(uid, List.of()).stream()
                        .map(AnalysisRequestService::toUserResultDTO)
                        .toList()))
                .toList();
    }

    /**
     * Computes live statistics for all of a customer's requests.
     * Returns {@link Optional#empty()} if the customer does not exist (maps to 404).
     * Mean and median are exact values derived from SUCCESS requests only.
     * Buckets use decade lower-bounds (20–80); ages below 20 are counted in the 20 bucket,
     * ages 80 and above are counted in the 80 bucket.
     */
    public Optional<StatisticsDTO> getStatistics(UUID customerId) {
        // 404 if the customer has never been seen
        if (!customerRepository.existsById(customerId)) {
            return Optional.empty();
        }

        List<AnalysisRequest> all = analysisRequestRepository.findByCustomerId(customerId);

        // "Number of all requests for this customer", any status
        int totalRequests = all.size();

        //Number of all users for this customer - distinct user IDs
        long totalUsers = all.stream()
                .map(AnalysisRequest::getUserId)
                .distinct()
                .count();

        // mean and median are computed only over SUCCESS requests (only they have ages)
        List<Double> ages = all.stream()
                .filter(r -> r.getStatus() == RequestStatus.SUCCESS)
                .map(AnalysisRequest::getAge)
                .filter(a -> a != null)
                .sorted()
                .toList();

        double mean = 0;
        double median = 0;

        if (!ages.isEmpty()) {
            // exact mean, sum / count
            mean = ages.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            // exact median, central point of sorted data
            int n = ages.size();
            if (n % 2 == 0) {
                median = (ages.get(n / 2 - 1) + ages.get(n / 2)) / 2.0;
            } else {
                median = ages.get(n / 2);
            }
        }

        // Buckets: keys 20–80 (step 10), always all 7 present.
        // bucket = floor(age/10)*10, capped to [20, 80]
        Map<Integer, Long> buckets = new LinkedHashMap<>();
        for (int i = 20; i <= 80; i += 10) {
            buckets.put(i, 0L);
        }
        for (Double age : ages) {
            int bucket = Math.min((int) (age / 10) * 10, 80);
            bucket = Math.max(bucket, 20);
            buckets.merge(bucket, 1L, Long::sum);
        }

        StatisticsContentsDTO contents = new StatisticsContentsDTO(
                (int) totalUsers, totalRequests, mean, median, buckets);

        // generated_at = now, since stats are always computed live
        return Optional.of(new StatisticsDTO(OffsetDateTime.now(), contents));
    }

    /**
     * Returns the user with all their request results for the given customer/user pair.
     * Returns {@link Optional#empty()} if no requests exist for that combination,
     * which the controller maps to a 404 response.
     */
    public Optional<UserDTO> getUserByCustomerAndUser(UUID customerId, UUID userId) {
        List<AnalysisRequest> requests = analysisRequestRepository
                .findByCustomerIdAndUserId(customerId, userId);
        if (requests.isEmpty()) return Optional.empty();
        return Optional.of(new UserDTO(userId, requests.stream()
                .map(AnalysisRequestService::toUserResultDTO)
                .toList()));
    }

    /** Maps an {@link AnalysisRequest} to the summarised {@link UserResultDTO} shape used inside user responses. */
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

    /**
     * Maps an {@link AnalysisRequest} entity to the full {@link AnalysisRequestDTO} API shape.
     * The {@code result} field is only populated when status is SUCCESS; it is null otherwise.
     */
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

    /**
     * A Pageable implementation that translates directly to SQL OFFSET/LIMIT, avoiding the
     * page-number arithmetic in PageRequest which only works when offset is a multiple of limit.
     * Spring Data JPA reads getOffset() and getPageSize() to build the OFFSET/LIMIT clauses.
     *
     */
    private record OffsetPageable(int offset, int limit, Sort sort) implements Pageable {
        @Override public int getPageNumber()          {
            return 0;
        }
        @Override public int getPageSize()            {
            return limit;
        }
        @Override public long getOffset()             {
            return offset;
        }
        @Override public Sort getSort()               {
            return sort;
        }
        @Override public Pageable next()              {
            throw new UnsupportedOperationException();
        }
        @Override public Pageable previousOrFirst()   {
            throw new UnsupportedOperationException();
        }
        @Override public Pageable first()             {
            throw new UnsupportedOperationException();
        }
        @Override public Pageable withPage(int p)     {
            throw new UnsupportedOperationException();
        }
        @Override public boolean hasPrevious()        {
            return false;
        }
    }
}
