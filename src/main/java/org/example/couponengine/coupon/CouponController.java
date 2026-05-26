package org.example.couponengine.coupon;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.example.couponengine.coupon.dto.CreateCouponRequest;
import org.example.couponengine.coupon.dto.RedeemCouponRequest;
import org.example.couponengine.coupon.dto.RedeemCouponResponse;
import org.example.couponengine.coupon.domain.Coupon;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/coupon")
@RestController()
@AllArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/create")
    @Operation(summary = "creates a coupon")
    @ApiResponse(responseCode = "204", description = "Coupon created successfully")
    public ResponseEntity<Void> createCoupon(@RequestBody CreateCouponRequest coupon) {
        couponService.save(Coupon.fromRequest(coupon));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeems coupon")
    public ResponseEntity<RedeemCouponResponse> redeem(
            @RequestBody RedeemCouponRequest redeemCouponRequest,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(
                couponService.redeemCoupon(redeemCouponRequest, extractIp(servletRequest))
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
