package com.tulipshipping.dto;

import lombok.Data;
import lombok.Getter;

import java.math.BigDecimal;

@Data
public class ShippingRequest {
    private String orderCode;       // Mã đơn hàng (VD: TULIP-2025...)
    private String receiverName;    // Tên người nhận
    private String receiverPhone;   // SĐT người nhận
    private String receiverAddress; // Địa chỉ đầy đủ để tính khoảng cách
    private String deliveryType;    // "STANDARD" hoặc "FAST"
    private BigDecimal codAmount;
}