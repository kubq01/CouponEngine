package org.example.couponengine.database;

import jakarta.persistence.*;
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
@Table(
        name = "coupon_usage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "coupon_user_usage",
                        columnNames = {"coupon_id", "user_id"}
                )
        }
)
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
