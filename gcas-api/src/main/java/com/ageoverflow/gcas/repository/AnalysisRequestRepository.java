package com.ageoverflow.gcas.repository;

import com.ageoverflow.gcas.model.AnalysisRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AnalysisRequestRepository extends JpaRepository<AnalysisRequest, UUID>,
        JpaSpecificationExecutor<AnalysisRequest> {
}
