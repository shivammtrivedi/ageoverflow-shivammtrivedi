package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for POST /analysis/{customer_id}/requests.
 * Carries the user identifier, priority flag, and base64-encoded photos to analyse.
 */
@Getter
@NoArgsConstructor
public class AnalysisRequestBody {

    @JsonProperty("user_id")
    private UUID userId;

    private boolean urgent;

    private List<String> photos;
}
