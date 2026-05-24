package org.example.couponengine.api;

public enum UseCouponResponse {
    SUCCESS,
    INVALID_COUNTRY_FAILURE,
    MAX_USAGES_REACHED_FAILURE,
    COUPON_NOT_FOUND_FAILURE,
    COUPON_ALREADY_USED_BY_USER_FAILURE
}
