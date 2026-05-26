package org.example.couponengine.coupon.exception;

public class CouponAlreadyExistsException extends RuntimeException {
    public CouponAlreadyExistsException(String id) {
        super(String.format("Coupon with id: %s already exists", id) );
    }
}
