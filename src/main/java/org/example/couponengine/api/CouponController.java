package org.example.couponengine.api;

import lombok.AllArgsConstructor;
import org.example.couponengine.service.Coupon;
import org.example.couponengine.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/coupon")
@RestController()
@AllArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/create")
    public ResponseEntity<Void> createCoupon(@RequestBody CreateCouponRequest coupon) {
        couponService.save(Coupon.fromRequest(coupon));
        return ResponseEntity.noContent().build();
    }
}
