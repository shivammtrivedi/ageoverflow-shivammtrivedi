package com.ageoverflow.gcas.repository;

import com.ageoverflow.gcas.model.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

/**
 * Persistence layer for {@link com.ageoverflow.gcas.model.AnalysisRequest} entities.
 * Extends {@link JpaSpecificationExecutor} to support dynamic filter queries
 * used by the list-requests endpoint.
 */
public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, UUID>,
        JpaSpecificationExecutor<AnalysisRequest> {

    /** Returns all requests belonging to the given customer, in no particular order. */
    List<AnalysisRequest> findByCustomerId(UUID customerId);

    /** Returns all requests belonging to the given customer and user pair. */
    List<AnalysisRequest> findByCustomerIdAndUserId(UUID customerId, UUID userId);
}
