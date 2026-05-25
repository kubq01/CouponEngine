package org.example.couponengine.service;

import lombok.AllArgsConstructor;
import org.example.couponengine.api.CouponId;
import org.example.couponengine.api.UseCouponResponse;
import org.example.couponengine.api.UserId;
import org.example.couponengine.database.CouponRepository;
import org.example.couponengine.database.CouponUsageRepository;
import org.example.couponengine.exceptions.CouponAlreadyExistsException;
import org.example.couponengine.geo.GeoMappingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.example.couponengine.api.UseCouponResponse.*;

@Service
@AllArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final GeoMappingService geoMappingService;

    public void save(Coupon coupon) {
        if (couponRepository.existsById(coupon.getId().toLowerCase())) {
            throw new CouponAlreadyExistsException(coupon.getId());
        }

        couponRepository.save(Coupon.toEntity(coupon));
    }

    @Transactional
    public UseCouponResponse useCoupon(CouponId couponId, String ip, UserId userId) {

        final var id = couponId.toString().toLowerCase();

        final var country = geoMappingService.getCountryCode(ip);

        final var coupon = couponRepository.findById(id)
                .orElse(null);

        if (coupon == null) {
            return COUPON_NOT_FOUND_FAILURE;
        }

        if (!coupon.getCountryCode().equalsIgnoreCase(country)) {
            return INVALID_COUNTRY_FAILURE;
        }

        int inserted = couponUsageRepository.insertIfNotExists(
                UUID.randomUUID(),
                id,
                userId.toString()
        );

        if (inserted == 0) {
            return COUPON_ALREADY_USED_BY_USER_FAILURE;
        }

        final int updated = couponRepository.incrementIfPossible(id);

        if (updated != 1) {
            return MAX_USAGES_REACHED_FAILURE;
        }

        return SUCCESS;
    }
}
