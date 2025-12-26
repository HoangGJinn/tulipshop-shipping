package com.tulipshipping.controller;

import com.tulipshipping.dto.ShippingRateRequest;
import com.tulipshipping.dto.ShippingRateResponse;
import com.tulipshipping.dto.ShippingRequest;
import com.tulipshipping.entity.ShippingOrder;
import com.tulipshipping.entity.enums.ShippingStatus;
import com.tulipshipping.repository.ShippingRepository;
import com.tulipshipping.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
@RestController
@RequestMapping("/api") // Base URL
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private ShippingRepository repository;

    // --- API 1: TÍNH PHÍ VẬN CHUYỂN (Đã cập nhật) ---
    @PostMapping("/shipping-method")
    public ResponseEntity<ShippingRateResponse> getShippingMethod(@RequestBody ShippingRateRequest request) {
        // 1. Lấy tọa độ & Tính khoảng cách
        double[] coords = shippingService.getCoordinates(request.getAddress());
        double distance = shippingService.calculateDistance(coords[0], coords[1]);

        // 2. Xác định phương thức giao hàng
        // Logic: Nếu FE có gửi lên thì dùng, nếu không gửi (null/rỗng) thì mặc định là STANDARD
        String method = (request.getDeliveryType() != null && !request.getDeliveryType().isEmpty())
                ? request.getDeliveryType().toUpperCase()
                : "STANDARD";

        // 3. Tính phí dựa trên method đã chọn
        BigDecimal fee = shippingService.calculateFee(distance, method);

        // 4. Trả về kết quả
        ShippingRateResponse response = new ShippingRateResponse(
                method, // Trả lại đúng method mà FE đã yêu cầu
                fee,
                Math.round(distance * 10.0) / 10.0,
                method.equals("FAST") ? "1-2 ngày" : "3-5 ngày" // Thời gian dự kiến đổi theo method
        );

        return ResponseEntity.ok(response);
    }

    // --- API 2: TẠO VẬN ĐƠN (ADD SHIPPING) ---
    // URL: POST http://localhost:8788/api/add-shipping
    @PostMapping("/add-shipping")
    public ResponseEntity<?> addShippingOrder(@RequestBody ShippingRequest request) {
        // 1. Tính toán lại khoảng cách & giá (Để bảo mật, không tin tưởng giá từ Client gửi lên)
        double[] coords = shippingService.getCoordinates(request.getReceiverAddress());
        double distance = shippingService.calculateDistance(coords[0], coords[1]);

        // Mặc định STANDARD nếu request không gửi, hoặc dùng giá trị request gửi
        String method = (request.getDeliveryType() == null || request.getDeliveryType().isEmpty())
                ? "STANDARD" : request.getDeliveryType();

        BigDecimal fee = shippingService.calculateFee(distance, method);

        // 2. Lưu vào Database
        ShippingOrder order = new ShippingOrder();
        order.setOrderCode(request.getOrderCode());
        order.setReceiverName(request.getReceiverName());
        order.setReceiverPhone(request.getReceiverPhone());
        order.setReceiverAddress(request.getReceiverAddress());

        order.setDistance(distance);
        order.setShippingPrice(fee);
        order.setDeliveryType(method);
        order.setStatus(ShippingStatus.PENDING); // Mới tạo thì là PENDING (Chờ lấy hàng)

        repository.save(order);

        // 3. Kích hoạt giả lập giao hàng (Chạy ngầm -> chuyển sang SHIPPED)
        shippingService.simulateDelivery(request.getOrderCode());

        return ResponseEntity.ok(order);
    }

    // --- API 3: THEO DÕI ĐƠN HÀNG (Giữ nguyên) ---
    @GetMapping("/track/{orderCode}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderCode) {
        return repository.findByOrderCode(orderCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}