package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
// Exclude null fields - checksum, primary_generation, and age are only present on success
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResultDTO {

    private UUID id;
    private String status;
    private String checksum;

    @JsonProperty("primary_generation")
    private String primaryGeneration;

    private Double age;
}
