package org.example.couponengine.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.couponengine.coupon.domain.CouponId;
import org.example.couponengine.coupon.domain.UserId;

@Schema(description = "Request used to redeem a coupon for a user")
public record RedeemCouponRequest(
        @Schema(description = "ID of the coupon being redeemed", example = "SUMMER")
        CouponId couponId,
        @Schema(description = "ID of the user redeeming the coupon", example = "user123")
        UserId userId) {
}
