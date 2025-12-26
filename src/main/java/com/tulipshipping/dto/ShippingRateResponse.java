package com.tulipshipping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ShippingRateResponse {
    private String method;          // VD: STANDARD
    private BigDecimal shippingFee; // Phí ship
    private double distance;        // Khoảng cách (km)
    private String estimatedTime;   // Thời gian dự kiến
}