package com.ageoverflow.gcas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private UUID id;
    private List<UserResultDTO> results;
}
