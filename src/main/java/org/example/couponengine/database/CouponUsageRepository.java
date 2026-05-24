package org.example.couponengine.database;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsageEntity, UUID> {
    Optional<CouponUsageEntity> findByCouponIdAndUserId(String couponId, String UserId);
}
