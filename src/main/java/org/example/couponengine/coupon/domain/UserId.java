package org.example.couponengine.coupon.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;
import org.example.couponengine.coupon.exception.InvalidRequestParameter;

public record UserId(String userId) {

    @JsonCreator
    public UserId {
        if (userId == null || !userId.matches("^[a-zA-Z0-9]+$")) {
            throw new InvalidRequestParameter(
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
