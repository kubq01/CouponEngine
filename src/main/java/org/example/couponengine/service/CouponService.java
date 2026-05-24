package org.example.couponengine.service;

import lombok.AllArgsConstructor;
import org.example.couponengine.api.CouponId;
import org.example.couponengine.api.UseCouponResponse;
import org.example.couponengine.database.CouponRepository;
import org.example.couponengine.exceptions.CouponAlreadyExistsException;
import org.example.couponengine.geo.GeoMappingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.example.couponengine.api.UseCouponResponse.*;

@Service
@AllArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final GeoMappingService geoMappingService;

    public void save(Coupon coupon) {
        if (couponRepository.existsById(coupon.getId())) {
            throw new CouponAlreadyExistsException(coupon.getId());
        }

        couponRepository.save(Coupon.toEntity(coupon));
    }

    @Transactional
    public UseCouponResponse useCoupon(CouponId couponId, String ip) {

        final var id = couponId.toString().toLowerCase();
        final var country = geoMappingService.getCountryCode(ip);

        final var existingCoupon = couponRepository.findById(id)
                .orElse(null);

        if (existingCoupon == null) {
            return COUPON_NOT_FOUND_FAILURE;
        }

        if (!existingCoupon.getCountryCode().equalsIgnoreCase(country)) {
            return INVALID_COUNTRY_FAILURE;
        }

        int updated = couponRepository.incrementIfPossible(id);

        if (updated == 1) {
            return SUCCESS;
        }

        return MAX_USAGES_REACHED_FAILURE;
    }
}
