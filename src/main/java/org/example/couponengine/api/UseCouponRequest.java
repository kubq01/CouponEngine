package org.example.couponengine.api;

public record UseCouponRequest(CouponId couponId, UserId userId) {
}
