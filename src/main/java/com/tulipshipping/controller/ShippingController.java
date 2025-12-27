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
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ShippingController {

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private ShippingRepository repository;

    // --- API 1: TÍNH PHÍ VẬN CHUYỂN ---
    @PostMapping("/shipping-method")
    public ResponseEntity<ShippingRateResponse> getShippingMethod(@RequestBody ShippingRateRequest request) {
        double[] coords = shippingService.getCoordinates(request.getAddress());
        double distance = shippingService.calculateDistance(coords[0], coords[1]);

        String method = (request.getDeliveryType() != null && !request.getDeliveryType().isEmpty())
                ? request.getDeliveryType().toUpperCase()
                : "STANDARD";

        BigDecimal fee = shippingService.calculateFee(distance, method);

        ShippingRateResponse response = new ShippingRateResponse(
                method,
                fee,
                Math.round(distance * 10.0) / 10.0,
                method.equals("FAST") ? "1-2 ngày" : "3-5 ngày"
        );

        return ResponseEntity.ok(response);
    }

    // --- API 2: TẠO VẬN ĐƠN (Chỉ tạo, chưa giao ngay) ---
    @PostMapping("/add-shipping")
    public ResponseEntity<?> addShippingOrder(@RequestBody ShippingRequest request) {
        // Có thể tái sử dụng logic createShippingOrder trong Service cho gọn
        // Nhưng nếu viết ở đây thì như sau:

        // 1. Tính toán lại khoảng cách & giá
        double[] coords = shippingService.getCoordinates(request.getReceiverAddress());
        double distance = shippingService.calculateDistance(coords[0], coords[1]);

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

        // QUAN TRỌNG: Trạng thái ban đầu là PENDING (Chờ shop giao hàng cho shipper)
        order.setStatus(ShippingStatus.PENDING);

        order.setCodAmount(request.getCodAmount());

        repository.save(order);

        return ResponseEntity.ok(order);
    }

    // API 3: KÍCH HOẠT GIAO HÀNG
    // URL: POST http://localhost:8788/api/start-delivery/ORD-12345
    @PostMapping("/start-delivery/{orderCode}")
    public ResponseEntity<?> startDelivery(@PathVariable String orderCode) {
        Optional<ShippingOrder> orderOpt = repository.findByOrderCode(orderCode);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ShippingOrder order = orderOpt.get();

        // Chỉ kích hoạt nếu đơn đang ở trạng thái chờ
        if (order.getStatus() == ShippingStatus.PENDING) {
            order.setStatus(ShippingStatus.SHIPPING); // Chuyển sang đang giao
            repository.save(order);

            // Chạy async giả lập giao hàng (sau 60s sẽ thành DELIVERED)
            shippingService.simulateDelivery(orderCode);

            return ResponseEntity.ok("Đơn hàng " + orderCode + " đã bắt đầu được giao!");
        } else {
            return ResponseEntity.badRequest().body("Đơn hàng không ở trạng thái chờ hoặc đã giao xong.");
        }
    }

    // --- API 4: THEO DÕI ĐƠN HÀNG ---
    @GetMapping("/track/{orderCode}")
    public ResponseEntity<?> trackOrder(@PathVariable String orderCode) {
        return repository.findByOrderCode(orderCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}