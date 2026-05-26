package org.example.couponengine.coupon;

import lombok.AllArgsConstructor;
import org.example.couponengine.coupon.dto.RedeemCouponRequest;
import org.example.couponengine.coupon.dto.RedeemCouponResponse;
import org.example.couponengine.coupon.persistence.CouponRepository;
import org.example.couponengine.coupon.persistence.CouponUsageRepository;
import org.example.couponengine.coupon.exception.CouponAlreadyExistsException;
import org.example.couponengine.geo.GeoMappingService;
import org.example.couponengine.coupon.domain.Coupon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.example.couponengine.coupon.dto.RedeemCouponResponse.*;

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
    public RedeemCouponResponse redeemCoupon(RedeemCouponRequest request, String ip) {

        final var id = request.couponId().toString().toLowerCase();
        final var userId = request.userId();
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

        final int updated = couponRepository.redeemIfPossible(id);

        if (updated != 1) {
            return MAX_USAGES_REACHED_FAILURE;
        }

        return SUCCESS;
    }
}
