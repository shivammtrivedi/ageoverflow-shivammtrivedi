package com.ageoverflow.gcas.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * API response shape for a single user.
 * Returned by GET /users and GET /users/{user_id}.
 * Bundles the user's UUID with the summarised results of all their analysis requests.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private UUID id;
    private List<UserResultDTO> results;
}
