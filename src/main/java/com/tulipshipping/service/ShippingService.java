package com.tulipshipping.service;

import java.math.BigDecimal;

public interface ShippingService {
    double[] getCoordinates(String address);

    double calculateDistance(double lat2, double lon2);

    BigDecimal calculateFee(double distance, String deliveryType);

    void simulateDelivery(String orderCode);
}
