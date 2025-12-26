package com.tulipshipping.entity;

import com.tulipshipping.entity.enums.ShippingStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipping_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShippingOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // QUAN TRỌNG: Mã đơn hàng từ tulipshop (thay cho vnp_txn_ref)
    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    // --- THÔNG TIN NGƯỜI GỬI (SHOP) ---
    private String senderName = "Tulip Shop - HCMUTE";
    private String senderPhone = "0909123456";
    private String senderAddress = "Số 1 Võ Văn Ngân, TP. Thủ Đức, TP. HCM";

    // --- THÔNG TIN NGƯỜI NHẬN (KHÁCH) ---
    private String receiverName;
    private String receiverPhone;

    @Column(columnDefinition = "TEXT")
    private String receiverAddress;

    // --- CHI TIẾT VẬN CHUYỂN ---
    private double distance; // km
    private BigDecimal shippingPrice;

    @Enumerated(EnumType.STRING)
    private ShippingStatus status;

    private String deliveryType; // STANDARD, FAST

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime deliveredAt;
}