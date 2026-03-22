package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The analysis result embedded inside a successful {@link AnalysisRequestDTO}.
 * Contains the engine checksum, per-generation confidence scores, the dominant generation, and estimated age.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResultDTO {

    private String checksum;
    private GenerationsDTO generations;

    @JsonProperty("primary_generation")
    private String primaryGeneration;

    private Double age;
}
