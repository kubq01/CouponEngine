package org.example.couponengine.coupon.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.couponengine.coupon.dto.CreateCouponRequest;
import org.example.couponengine.commons.InvalidRequestParameter;
import org.example.couponengine.coupon.persistence.CouponEntity;
import org.example.couponengine.geo.CountryCode;

import java.time.Instant;

@AllArgsConstructor
@Getter
public class Coupon {
    CouponId id;
    Instant createdAt;
    int maxUsages;
    int currentUsages;
    CountryCode countryCode;

    public static CouponEntity toEntity(Coupon coupon) {
        return new CouponEntity(
                coupon.getId().getNormalizedId(),
                coupon.createdAt,
                coupon.maxUsages,
                coupon.currentUsages,
                coupon.countryCode.toString()
        );
    }

    public static Coupon fromRequest(CreateCouponRequest request) {
        if(request.maxUsages() < 1) {
            throw new InvalidRequestParameter("Max usages of the coupon cannot be smaller than 1");
        }

        return new Coupon(
                request.id(),
                request.createdAt(),
                request.maxUsages(),
                0,
                request.countryCode()
        );
    }
}
