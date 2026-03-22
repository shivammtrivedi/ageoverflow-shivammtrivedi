package com.ageoverflow.gcas.dto;

import com.ageoverflow.gcas.model.RequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response shape for a single analysis request.
 * Returned by POST /requests (on creation) and GET /requests/{id}.
 * The {@code result} field is omitted from JSON when null (i.e. status is not success).
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisRequestDTO {

    private UUID id;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    private RequestStatus status;

    // Only included in JSON when status is success, null otherwise
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ResultDTO result;
}
