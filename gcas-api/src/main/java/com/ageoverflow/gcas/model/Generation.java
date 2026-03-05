package com.ageoverflow.gcas.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Generation {
    SILENT("silent"),
    BABY_BOOMERS("baby_boomers"),
    X("x"),
    Y("y"),
    Z("z"),
    ALPHA("alpha");

    private final String value;

    Generation(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Generation fromValue(String value) {
        for (Generation g : values()) {
            if (g.value.equals(value)) return g;
        }
        throw new IllegalArgumentException("Unknown generation: " + value);
    }
}
