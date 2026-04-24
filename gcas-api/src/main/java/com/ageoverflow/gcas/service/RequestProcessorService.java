package com.ageoverflow.gcas.service;

import com.ageoverflow.gcas.exception.EngineException;
import com.ageoverflow.gcas.model.AnalysisRequest;
import com.ageoverflow.gcas.model.Generation;
import com.ageoverflow.gcas.model.RequestStatus;
import com.ageoverflow.gcas.repository.AnalysisRequestRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Handles asynchronous execution of the analysis engine for a saved request.
 * Invoked after a request has been persisted so the caller receives a response immediately
 * while processing continues in a background thread.
 */
@Service
public class RequestProcessorService {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final EngineService engineService;

    public RequestProcessorService(AnalysisRequestRepository analysisRequestRepository,
                                   EngineService engineService) {
        this.analysisRequestRepository = analysisRequestRepository;
        this.engineService = engineService;
    }

    /**
     * Runs the engine for the given request in a background thread.
     * On success, persists all result fields and sets status to SUCCESS.
     * On engine failure, sets status to FAILED and saves without result data.
     * The {@code @Transactional} annotation ensures the final status update is atomic.
     */
    @Async("engineExecutor")
    @Transactional
    public void processAsync(UUID requestId) {
        AnalysisRequest request = analysisRequestRepository.findById(requestId).orElse(null);
        if (request == null) {
            return;
        }

        try {
            EngineService.EngineResult result = engineService.compute(requestId, request.getPhotos());

            request.setChecksum(result.checksum());
            request.setAge(result.age());
            request.setPrimaryGeneration(Generation.fromValue(result.primaryGeneration()));
            request.setGenSilent(result.generations().silent());
            request.setGenBabyBoomers(result.generations().babyBoomers());
            request.setGenX(result.generations().x());
            request.setGenY(result.generations().y());
            request.setGenZ(result.generations().z());
            request.setGenAlpha(result.generations().alpha());
            request.setStatus(RequestStatus.SUCCESS);
        } catch (EngineException e) {
            request.setStatus(RequestStatus.FAILED);
        }

        analysisRequestRepository.save(request);
    }
}
