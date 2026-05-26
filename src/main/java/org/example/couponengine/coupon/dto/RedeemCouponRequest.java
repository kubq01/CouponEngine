package org.example.couponengine.coupon.dto;

import org.example.couponengine.coupon.domain.CouponId;
import org.example.couponengine.coupon.domain.UserId;

public record RedeemCouponRequest(CouponId couponId, UserId userId) {
}
