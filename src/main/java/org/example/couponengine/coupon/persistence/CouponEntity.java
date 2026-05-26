package org.example.couponengine.coupon.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CouponEntity {
    @Id
    String id;
    Instant createdAt;
    int maxUsages;
    int currentUsages;
    String countryCode;
}
