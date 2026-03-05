package com.ageoverflow.gcas.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
