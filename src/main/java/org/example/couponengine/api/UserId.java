package org.example.couponengine.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;

public record UserId(String userId) {

    @JsonCreator
    public UserId {
        if (userId == null || !userId.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException(
                    "UserId must contain only letters and numbers"
            );
        }
    }

    @JsonValue
    public String userId() {
        return userId;
    }

    @Override
    @NonNull
    public String toString() {
        return userId;
    }
}
