package org.example.couponengine.coupon.exception;

public class InvalidRequestParameter extends RuntimeException {
    public InvalidRequestParameter(String message) {
        super(message);
    }
}
