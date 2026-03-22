package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Confidence percentages (0–100) for each generational cohort as returned by the engine.
 * All seven fields are always populated on a successful analysis.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GenerationsDTO {

    private Double silent;

    @JsonProperty("baby_boomers")
    private Double babyBoomers;

    private Double x;
    private Double y;
    private Double z;
    private Double alpha;
}
