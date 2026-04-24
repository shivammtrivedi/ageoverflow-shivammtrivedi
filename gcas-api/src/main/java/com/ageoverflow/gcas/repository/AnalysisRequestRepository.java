package com.ageoverflow.gcas.repository;

import com.ageoverflow.gcas.model.AnalysisRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Returns a paginated list of distinct user IDs for a customer, ordered by user_id for
     * stable pagination. Using a @Query so the DISTINCT + ORDER BY go to the DB, not Java.
     */
    @Query("SELECT DISTINCT r.userId FROM AnalysisRequest r WHERE r.customer.id = :customerId ORDER BY r.userId")
    List<UUID> findDistinctUserIdsByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /** Fetches all requests for a customer belonging to any of the given user IDs. */
    List<AnalysisRequest> findByCustomerIdAndUserIdIn(UUID customerId, List<UUID> userIds);
}
