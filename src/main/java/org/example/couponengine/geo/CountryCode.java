package org.example.couponengine.geo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.NonNull;

public record CountryCode(String countryCode) {
    @JsonCreator
    public CountryCode {
        if (countryCode == null || !countryCode.matches("^[a-zA-Z]+$") || countryCode.length() > 10) {
            throw new IllegalArgumentException("Country code is invalid");
        }
    }

    @JsonValue
    public String countryCode() {
        return countryCode;
    }

    @Override
    @NonNull
    public String toString() {
        return countryCode;
    }
}
