package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Summarised view of a single analysis request as it appears inside a {@link UserDTO}.
 * Null fields (checksum, primary_generation, age) are serialised as JSON null
 * when the request has not yet succeeded, so all keys are always present in the response.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserResultDTO {

    private UUID id;
    private String status;
    private String checksum;

    @JsonProperty("primary_generation")
    private String primaryGeneration;

    private Double age;
}
