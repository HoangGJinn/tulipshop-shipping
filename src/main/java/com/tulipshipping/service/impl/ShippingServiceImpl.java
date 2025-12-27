package com.tulipshipping.service.impl;

import com.tulipshipping.dto.ShippingRequest;
import com.tulipshipping.entity.ShippingOrder;
import com.tulipshipping.entity.enums.ShippingStatus;
import com.tulipshipping.repository.ShippingRepository;
import com.tulipshipping.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ShippingServiceImpl implements ShippingService {

    private final double SHOP_LAT = 10.8507; // HCMUTE
    private final double SHOP_LON = 106.7720;
    private final String LOCATION_IQ_TOKEN = "pk.b4434589a503285f90565b10d3ef7b12";

    @Autowired
    private ShippingRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    // --- 1. HÀM TẠO ĐƠN (QUAN TRỌNG: Đã thêm mapping codAmount) ---
    @Override
    public ShippingOrder createShippingOrder(ShippingRequest request) {
        // 1. Lấy tọa độ người nhận
        double[] coords = getCoordinates(request.getReceiverAddress());

        // 2. Tính khoảng cách
        double distance = calculateDistance(coords[0], coords[1]);

        // 3. Tính phí ship
        BigDecimal shippingFee = calculateFee(distance, request.getDeliveryType());

        // 4. Tạo Entity và lưu
        ShippingOrder order = new ShippingOrder();
        order.setOrderCode(request.getOrderCode());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());
        order.setDeliveryType(request.getDeliveryType());

        // [QUAN TRỌNG] Lưu số tiền thu hộ (COD)
        // Nếu null thì gán bằng 0
        order.setCodAmount(request.getCodAmount() != null ? request.getCodAmount() : BigDecimal.ZERO);

        // Các thông tin tính toán
        order.setDistance(Math.round(distance * 100.0) / 100.0); // Làm tròn 2 số lẻ
        order.setShippingPrice(shippingFee);

        // Trạng thái mặc định
        order.setStatus(ShippingStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        // Lưu vào DB
        return repository.save(order);
    }

    // --- 2. CÁC HÀM CŨ CỦA BẠN (GIỮ NGUYÊN VÌ ĐÃ ỔN) ---

    @Override
    public double[] getCoordinates(String address) {
        if (address == null || address.trim().isEmpty()) {
            return getFallbackCoordinates("unknown");
        }

        try {
            String url = "https://us1.locationiq.com/v1/search?key={key}&q={q}&format=json&limit=1";
            Map<String, String> params = new java.util.HashMap<>();
            params.put("key", LOCATION_IQ_TOKEN);
            params.put("q", address);

            // System.out.println(">>> Đang gọi API cho địa chỉ: " + address);
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class, params);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get(0);
                double lat = Double.parseDouble((String) data.get("lat"));
                double lon = Double.parseDouble((String) data.get("lon"));
                return new double[]{lat, lon};
            }

        } catch (Exception e) {
            System.err.println("Lỗi LocationIQ: " + e.getMessage());
        }
        return getFallbackCoordinates(address);
    }

    private double[] getFallbackCoordinates(String address) {
        System.out.println(">>> Fallback Mode cho địa chỉ: " + address);
        String addrLower = address.toLowerCase();
        if (addrLower.contains("hà nội")) return new double[]{21.0285, 105.8542};
        if (addrLower.contains("đà nẵng")) return new double[]{16.0544, 108.2022};
        if (addrLower.contains("quận 1")) return new double[]{10.7769, 106.7009};
        return new double[]{10.8400, 106.7600}; // Default HCMUTE area
    }

    @Override
    public double calculateDistance(double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - SHOP_LAT);
        double dLon = Math.toRadians(lon2 - SHOP_LON);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(SHOP_LAT)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public BigDecimal calculateFee(double distance, String deliveryType) {
        BigDecimal price;
        if (distance < 10) price = new BigDecimal("15000");
        else if (distance < 50) price = new BigDecimal("25000");
        else if (distance < 300) price = new BigDecimal("32000");
        else price = new BigDecimal("38000");

        if ("FAST".equalsIgnoreCase(deliveryType)) {
            price = price.add(new BigDecimal("15000"));
        }

        if (distance > 300) {
            double extraKm = distance - 300;
            BigDecimal surcharge = BigDecimal.valueOf(Math.floor(extraKm / 100) * 1000);
            price = price.add(surcharge);
        }
        return price;
    }


    @Override
    @Async
    public void simulateDelivery(String orderCode) {
        try {
            System.out.println(">>> [Shipping] Bắt đầu giao đơn: " + orderCode);
            // Giả lập giao hàng 30 giây (giảm xuống cho nhanh test)
            Thread.sleep(30000);

            repository.findByOrderCode(orderCode).ifPresent(order -> {
                // 1. Cập nhật nội bộ Shipping
                order.setStatus(ShippingStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                repository.save(order);
                System.out.println(">>> [Shipping] Giao thành công đơn: " + orderCode);

                // 2. GỌI WEBHOOK SANG TULIP SHOP
                notifyTulipShop(orderCode, "DELIVERED");
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Hàm phụ trợ gọi API Shop
    private void notifyTulipShop(String orderCode, String status) {
        try {
            String shopUrl = "http://localhost:8787/v1/api/webhook/shipping-update"; // URL của Shop

            // Tạo body JSON thủ công hoặc dùng Map
            Map<String, String> body = new HashMap<>();
            body.put("orderCode", orderCode);
            body.put("status", status);
            body.put("message", "Giao hàng thành công bởi TulipShipping");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            // Gọi POST
            restTemplate.postForEntity(shopUrl, request, String.class);
            System.out.println(">>> [Shipping] Đã thông báo cho TulipShop thành công!");

        } catch (Exception e) {
            System.err.println(">>> [Shipping] Lỗi khi gọi Webhook Shop: " + e.getMessage());
            // Thực tế có thể thêm logic retry (thử lại) nếu gọi lỗi
        }
    }
}