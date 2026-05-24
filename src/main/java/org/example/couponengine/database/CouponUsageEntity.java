package org.example.couponengine.database;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.example.couponengine.api.CouponId;
import org.example.couponengine.api.UserId;

import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CouponUsageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String couponId;
    private String userId;

    public static CouponUsageEntity createEntity(CouponId couponId, UserId userId) {
       return CouponUsageEntity.builder()
               .couponId(couponId.toString())
               .userId(userId.toString()).build();
    }
}
