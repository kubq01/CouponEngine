package org.example.couponengine.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.example.couponengine.service.Coupon;
import org.example.couponengine.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/use/{couponId}")
    public ResponseEntity<UseCouponResponse> use(
            @PathVariable CouponId couponId,
            HttpServletRequest request
    ) {
        String ip = extractIp(request);

        return ResponseEntity.ok(
                couponService.useCoupon(couponId, ip)
        );
    }

    private String extractIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
