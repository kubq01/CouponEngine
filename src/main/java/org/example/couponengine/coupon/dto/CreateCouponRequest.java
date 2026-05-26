package org.example.couponengine.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.example.couponengine.coupon.domain.CouponId;
import org.example.couponengine.geo.CountryCode;

import java.time.Instant;

@Schema(description = "Request used to create a new coupon")
public record CreateCouponRequest(
        @Schema(description = "Unique identifier of the coupon", example = "SUMMER")
        CouponId id,
        @Schema(description = "Timestamp when coupon was created", example = "2026-05-26T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Maximum number of times coupon can be used", example = "100")
        int maxUsages,
        @Schema(description = "Country where coupon is valid", example = "PL")
        CountryCode countryCode
) {}
