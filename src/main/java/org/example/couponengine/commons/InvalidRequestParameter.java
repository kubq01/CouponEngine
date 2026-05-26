package org.example.couponengine.commons;

public class InvalidRequestParameter extends RuntimeException {
    public InvalidRequestParameter(String message) {
        super(message);
    }
}
