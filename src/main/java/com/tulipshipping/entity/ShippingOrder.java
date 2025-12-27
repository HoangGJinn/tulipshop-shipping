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
@Builder // Thêm Builder để dễ tạo object
public class ShippingOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã đơn hàng từ tulipshop
    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    // --- THÔNG TIN NGƯỜI GỬI (SHOP) ---
    @Builder.Default
    private String senderName = "Tulip Shop - HCMUTE";
    @Builder.Default
    private String senderPhone = "0909123456";
    @Builder.Default
    private String senderAddress = "Số 1 Võ Văn Ngân, TP. Thủ Đức, TP. HCM";

    // --- THÔNG TIN NGƯỜI NHẬN (KHÁCH) ---
    private String receiverName;
    private String receiverPhone;

    @Column(columnDefinition = "TEXT")
    private String receiverAddress;

    // --- CHI TIẾT VẬN CHUYỂN ---
    private double distance; // km
    private BigDecimal shippingPrice; // Phí ship

    // [MỚI] Số tiền thu hộ (COD)
    // Nếu khách thanh toán online (VNPAY/MOMO) thì giá trị này là 0
    @Column(name = "cod_amount")
    private BigDecimal codAmount;

    @Enumerated(EnumType.STRING)
    private ShippingStatus status;

    private String deliveryType; // STANDARD, FAST

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime deliveredAt;
}