package org.example.couponengine.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CouponUsageRepository extends JpaRepository<CouponUsageEntity, UUID> {
    Optional<CouponUsageEntity> findByCouponIdAndUserId(String couponId, String UserId);

    @Modifying
    @Query(value = """
        INSERT INTO coupon_usage (id, coupon_id, user_id)
        VALUES (:id, :couponId, :userId)
        ON CONFLICT (coupon_id, user_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfNotExists(@Param("id") UUID id,
                          @Param("couponId") String couponId,
                          @Param("userId") String userId);
}
