package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
