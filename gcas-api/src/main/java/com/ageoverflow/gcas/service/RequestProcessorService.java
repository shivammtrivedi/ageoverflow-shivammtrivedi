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

@Service
public class RequestProcessorService {

    private final AnalysisRequestRepository analysisRequestRepository;
    private final EngineService engineService;

    public RequestProcessorService(AnalysisRequestRepository analysisRequestRepository,
                                   EngineService engineService) {
        this.analysisRequestRepository = analysisRequestRepository;
        this.engineService = engineService;
    }

    @Async
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
