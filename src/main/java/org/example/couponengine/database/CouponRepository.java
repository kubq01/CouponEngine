package org.example.couponengine.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<CouponEntity, String> {
    boolean existsById(String id);

    @Modifying
    @Query("""
            UPDATE CouponEntity c
            SET c.currentUsages = c.currentUsages + 1
            WHERE c.id = :id AND c.currentUsages < c.maxUsages
            """)
    int incrementIfPossible(@Param("id") String id);
}
