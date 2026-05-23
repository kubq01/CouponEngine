package org.example.couponengine.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record CouponId(String id) {

    @JsonCreator
    public CouponId {
        if (id == null || !id.matches("^[a-zA-Z0-9]+$")) {
            throw new IllegalArgumentException(
                    "CouponId must contain only letters and numbers"
            );
        }
    }

    @JsonValue
    public String id() {
        return id;
    }
}
