package com.tulipshipping.dto;

import lombok.Data;

@Data
public class ShippingRateRequest {
    private String address;
    private String deliveryType;
}
