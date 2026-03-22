package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Top-level response for GET /analysis/{customer_id}/statistics.
 * Wraps the statistics payload with a timestamp indicating when the report was generated.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsDTO {

    // The date and time (RFC3339) for which these statistics are valid.
    // Using current time since our stats are always live (computed on request).
    @JsonProperty("generated_at")
    private OffsetDateTime generatedAt;

    private StatisticsContentsDTO contents;
}
