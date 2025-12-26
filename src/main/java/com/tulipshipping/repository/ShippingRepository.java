package com.tulipshipping.repository;

import com.tulipshipping.entity.ShippingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ShippingRepository extends JpaRepository<ShippingOrder, Long> {
    Optional<ShippingOrder> findByOrderCode(String orderCode);
}