package org.example.couponengine.commons;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(InvalidRequestParameter.class)
    public ResponseEntity<String> handleInvalidRequestParameter(InvalidRequestParameter ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CouponAlreadyExistsException.class)
    public ResponseEntity<String> handleCouponAlreadyExistsException(CouponAlreadyExistsException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(GeoLookupException.class)
    public ResponseEntity<String> handleGeoLookupException(GeoLookupException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
