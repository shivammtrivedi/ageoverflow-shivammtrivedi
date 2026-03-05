package com.ageoverflow.gcas.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RequestStatus {
    PENDING, SUCCESS, FAILED;

    @JsonValue
    public String toLowerCase() {
        return name().toLowerCase();
    }
}
