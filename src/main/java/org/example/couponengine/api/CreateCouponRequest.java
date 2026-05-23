package org.example.couponengine.api;

import java.time.Instant;

public record CreateCouponRequest(
        CouponId id,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        String countryCode
) {}
