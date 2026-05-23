package org.example.couponengine.service;

import lombok.AllArgsConstructor;
import org.example.couponengine.api.CreateCouponRequest;
import org.example.couponengine.database.CouponEntity;
import org.example.couponengine.api.CouponId;

import java.time.Instant;

@AllArgsConstructor
public class Coupon {
    CouponId id;
    Instant createdAt;
    int maxUsages;
    int currentUsages;
    String countryCode;

    public static CouponEntity toEntity(Coupon coupon) {
        return new CouponEntity(
                coupon.getId().toLowerCase(),
                coupon.createdAt,
                coupon.maxUsages,
                coupon.currentUsages,
                coupon.countryCode
        );
    }

    public static Coupon fromRequest(CreateCouponRequest request) {
        return new Coupon(
                request.id(),
                request.createdAt(),
                request.maxUsages(),
                request.currentUsages(),
                request.countryCode()
        );
    }

    public String getId() {
        return this.id.id();
    }
}
