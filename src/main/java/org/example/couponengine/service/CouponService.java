package org.example.couponengine.service;

import lombok.AllArgsConstructor;
import org.example.couponengine.database.CouponRepository;
import org.example.couponengine.exceptions.CouponAlreadyExistsException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public void save(Coupon coupon) {
        if (couponRepository.existsById(coupon.getId())) {
            throw new CouponAlreadyExistsException(coupon.getId());
        }

        couponRepository.save(Coupon.toEntity(coupon));
    }
}
