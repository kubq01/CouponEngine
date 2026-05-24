package org.example.couponengine.api;

import org.example.couponengine.geo.CountryCode;

import java.time.Instant;

public record CreateCouponRequest(
        CouponId id,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        CountryCode countryCode
) {}
