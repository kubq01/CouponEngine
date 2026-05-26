package org.example.couponengine.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of coupon redemption attempt")
public enum RedeemCouponResponse {
    @Schema(description = "Coupon successfully redeemed")
    SUCCESS,
    @Schema(description = "Coupon is not valid for user's country")
    INVALID_COUNTRY_FAILURE,
    @Schema(description = "Coupon reached maximum allowed usages")
    MAX_USAGES_REACHED_FAILURE,
    @Schema(description = "Coupon does not exist")
    COUPON_NOT_FOUND_FAILURE,
    @Schema(description = "User already used this coupon")
    COUPON_ALREADY_USED_BY_USER_FAILURE
}