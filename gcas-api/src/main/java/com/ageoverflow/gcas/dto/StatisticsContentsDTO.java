package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * The statistics payload nested inside {@link StatisticsDTO}.
 * Covers all requests for a customer regardless of status, with mean/median/buckets
 * derived only from SUCCESS requests (the only ones with valid age values).
 * Bucket keys are the decade lower-bounds 20–80; all seven keys are always present.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsContentsDTO {

    @JsonProperty("total_users")
    private int totalUsers;

    @JsonProperty("total_requests")
    private int totalRequests;

    private double mean;
    private double median;

    // Keys are bucket lower bounds: 20, 30, 40, 50, 60, 70, 80
    private Map<Integer, Long> buckets;
}
