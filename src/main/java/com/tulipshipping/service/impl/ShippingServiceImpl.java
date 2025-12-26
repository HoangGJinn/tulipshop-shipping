package com.tulipshipping.service.impl;

import com.tulipshipping.entity.ShippingOrder;
import com.tulipshipping.entity.enums.ShippingStatus;
import com.tulipshipping.repository.ShippingRepository;
import com.tulipshipping.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ShippingServiceImpl implements ShippingService {

    private final double SHOP_LAT = 10.8507; // HCMUTE
    private final double SHOP_LON = 106.7720;

    // Đã thêm dấu ngoặc kép "" cho chuỗi String
    private final String LOCATION_IQ_TOKEN = "pk.b4434589a503285f90565b10d3ef7b12";

    @Autowired
    private ShippingRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public double[] getCoordinates(String address) {
        // 1. Kiểm tra đầu vào
        if (address == null || address.trim().isEmpty()) {
            return getFallbackCoordinates("unknown");
        }

        try {
            // 2. Tạo URL với các placeholder (dấu ngoặc nhọn {})
            // Spring sẽ tự động điền giá trị vào các chỗ {key}, {q} này và mã hóa chuẩn xác.
            String url = "https://us1.locationiq.com/v1/search?key={key}&q={q}&format=json&limit=1";

            // 3. Tạo Map chứa các tham số
            Map<String, String> params = new java.util.HashMap<>();
            params.put("key", LOCATION_IQ_TOKEN);
            params.put("q", address); // Truyền địa chỉ gốc (có dấu, khoảng trắng thoải mái)

            System.out.println(">>> Đang gọi API cho địa chỉ: " + address);

            // 4. Gọi API với params (RestTemplate sẽ tự xử lý encoding)
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class, params);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get(0);
                double lat = Double.parseDouble((String) data.get("lat"));
                double lon = Double.parseDouble((String) data.get("lon"));

                System.out.println(">>> Kết quả: Lat=" + lat + " Lon=" + lon);
                return new double[]{lat, lon};
            }

        } catch (Exception e) {
            // In lỗi chi tiết
            System.err.println("Lỗi LocationIQ: " + e.getMessage());
        }

        return getFallbackCoordinates(address);
    }

    // Hàm giả lập tọa độ khi API lỗi hoặc hết quota
    private double[] getFallbackCoordinates(String address) {
        System.out.println(">>> Đang sử dụng tọa độ giả lập (Fallback Mode)");
        String addrLower = address.toLowerCase();

        if (addrLower.contains("hà nội")) return new double[]{21.0285, 105.8542};
        if (addrLower.contains("đà nẵng")) return new double[]{16.0544, 108.2022};
        if (addrLower.contains("quận 1")) return new double[]{10.7769, 106.7009};

        // Mặc định trả về gần Thủ Đức
        return new double[]{10.8400, 106.7600};
    }

    @Override
    public double calculateDistance(double lat2, double lon2) {
        double R = 6371; // Bán kính trái đất (km)
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

        // Logic tính giá cũ của bạn vẫn rất ổn
        if (distance < 10) {
            price = new BigDecimal("15000");
        } else if (distance < 50) {
            price = new BigDecimal("25000");
        } else if (distance < 300) {
            price = new BigDecimal("32000");
        } else {
            price = new BigDecimal("38000");
        }

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
            System.out.println(">>> Đơn hàng " + orderCode + " đang được giao...");
            // Giả lập giao hàng mất 60s
            Thread.sleep(60000);

            repository.findByOrderCode(orderCode).ifPresent(order -> {
                order.setStatus(ShippingStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                repository.save(order);
                System.out.println(">>> Đơn hàng " + orderCode + " đã giao THÀNH CÔNG!");
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}