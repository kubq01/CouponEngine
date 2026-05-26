package org.example.couponengine.coupon.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsageEntity, UUID> {

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
