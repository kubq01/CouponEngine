package org.example.couponengine.coupon.dto;

import org.example.couponengine.coupon.domain.CouponId;
import org.example.couponengine.geo.CountryCode;

import java.time.Instant;

public record CreateCouponRequest(
        CouponId id,
        Instant createdAt,
        int maxUsages,
        int currentUsages,
        CountryCode countryCode
) {}
