package com.ageoverflow.gcas.repository;

import com.ageoverflow.gcas.model.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, UUID>,
        JpaSpecificationExecutor<AnalysisRequest> {
        List<AnalysisRequest> findByCustomerId(UUID customerId);
        List<AnalysisRequest> findByCustomerIdAndUserId(UUID customerId, UUID userId);
}
