package com.example.paymentservice.service;

import com.example.paymentservice.dto.PaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.event.OrderCreatedEvent;
import com.example.paymentservice.event.PaymentCompletedEvent;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient paymentGatewayClient;
    private final Environment environment; // Environment 객체 추가

    @Value("${payment.gateway.api-key}")
    private String paymentGatewayApiKey;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, KafkaTemplate<String, Object> kafkaTemplate, WebClient paymentGatewayClient, Environment environment) {
        this.paymentRepository = paymentRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.paymentGatewayClient = paymentGatewayClient;
        this.environment = environment;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        logger.info("Processing payment for order: {}", paymentRequest.getOrderNumber());

        // 새 결제 생성
        Payment payment = new Payment();
        payment.setOrderNumber(paymentRequest.getOrderNumber());
        payment.setAmount(paymentRequest.getAmount());
        payment.setPaymentMethod(paymentRequest.getPaymentMethod());
        payment.setStatus(PaymentStatus.PROCESSING);

        Payment savedPayment = paymentRepository.save(payment);

        // 개발 환경인 경우 모의 결제 처리
        if (isDevelopmentEnvironment()) {
            mockPaymentProcessing(savedPayment);
        } else {
            // 실제 결제 게이트웨이 호출
            processPaymentWithGateway(savedPayment);
        }

        return mapToPaymentResponse(savedPayment);
    }

    // 현재 환경이 개발 환경인지 확인하는 메소드
    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length > 0 &&
                (Arrays.asList(activeProfiles).contains("dev") ||
                        Arrays.asList(activeProfiles).contains("local"));
    }

    private void mockPaymentProcessing(Payment payment) {
        // 개발 환경에서 결제 처리 시뮬레이션
        try {
            // 처리 시간 시뮬레이션
            Thread.sleep(new Random().nextInt(1000));

            // 90%의 확률로 성공
            boolean isSuccessful = new Random().nextDouble() > 0.1;

            if (isSuccessful) {
                payment.markAsCompleted();
                payment.setPaymentGatewayResponse("SANDBOX: Payment processed successfully");

                // 결제 완료 이벤트 발행
                PaymentCompletedEvent event = new PaymentCompletedEvent(
                        payment.getPaymentId(),
                        payment.getOrderNumber(),
                        payment.getAmount(),
                        PaymentStatus.COMPLETED.name(),
                        payment.getUpdatedAt()
                );

                kafkaTemplate.send("payment-completed-topic", event);
                logger.info("SANDBOX: Payment completed for order: {}", payment.getOrderNumber());
            } else {
                payment.markAsFailed("SANDBOX: Payment gateway declined the transaction");
                logger.warn("SANDBOX: Payment failed for order: {}", payment.getOrderNumber());
            }

            paymentRepository.save(payment);
        } catch (Exception e) {
            payment.markAsFailed("SANDBOX: Payment processing error: " + e.getMessage());
            paymentRepository.save(payment);
            logger.error("SANDBOX: Error processing payment: ", e);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        return mapToPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderNumber(String orderNumber) {
        List<Payment> payments = paymentRepository.findByOrderNumber(orderNumber);

        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @KafkaListener(topics = "order-created-topic", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        logger.info("Received order created event: {}", event);

        try {
            // 이미 이 주문에 대한 결제가 있는지 확인
            List<Payment> existingPayments = paymentRepository.findByOrderNumber(event.getOrderNumber());
            if (!existingPayments.isEmpty()) {
                logger.warn("Payment for order {} already exists. Ignoring duplicate event.", event.getOrderNumber());
                return;
            }

            // 새 결제 생성
            Payment payment = new Payment();
            payment.setOrderNumber(event.getOrderNumber());
            payment.setAmount(event.getTotalAmount());
            payment.setPaymentMethod(event.getPaymentMethod() != null ? event.getPaymentMethod() : "CARD"); // 기본값 설정
            payment.setStatus(PaymentStatus.PENDING);

            Payment savedPayment = paymentRepository.save(payment);
            logger.info("Created new pending payment: {} for order: {}", savedPayment.getPaymentId(), event.getOrderNumber());

            // 자동 결제 처리 (옵션)
            // 실제 구현에서는 자동으로 결제를 진행하지 않고 사용자가 명시적으로 결제를 진행하도록 할 수도 있음
            if (shouldAutoProcessPayment(event)) {
                logger.info("Auto-processing payment for order: {}", event.getOrderNumber());

                // 개발 환경인 경우 모의 결제 처리
                if (isDevelopmentEnvironment()) {
                    mockPaymentProcessing(savedPayment);
                } else {
                    // 실제 결제 게이트웨이 호출
                    processPaymentWithGateway(savedPayment);
                }
            }
        } catch (Exception e) {
            logger.error("Error handling order created event: ", e);
        }
    }

    // 자동 결제 처리 여부 결정 메소드
    private boolean shouldAutoProcessPayment(OrderCreatedEvent event) {
        // 여기에 비즈니스 로직 구현
        // 예: 특정 조건에 따라 자동 결제 처리 여부 결정
        // - 결제 방법이 "AUTO_PAYMENT"인 경우
        // - 총액이 특정 금액 이하인 경우
        // - 특정 고객 등급인 경우

        // 간단한 예시: 항상 false (자동 결제 처리 안 함)
        return false;

        // 또는 특정 조건에 따라 결정
        // return "AUTO_PAYMENT".equals(event.getPaymentMethod()) || event.getTotalAmount().compareTo(new BigDecimal("1000")) <= 0;
    }

    // 결제 게이트웨이 호출 (예시)
    private void processPaymentWithGateway(Payment payment) {
        try {
            // 실제 결제 게이트웨이 호출 대신 성공/실패를 랜덤하게 시뮬레이션
            boolean isSuccessful = new Random().nextDouble() > 0.1; // 90% 확률로 성공

            if (isSuccessful) {
                payment.markAsCompleted();
                payment.setPaymentGatewayResponse("Payment processed successfully");

                // 결제 완료 이벤트 발행
                PaymentCompletedEvent event = new PaymentCompletedEvent(
                        payment.getPaymentId(),
                        payment.getOrderNumber(),
                        payment.getAmount(),
                        PaymentStatus.COMPLETED.name(),
                        payment.getUpdatedAt()
                );

                kafkaTemplate.send("payment-completed-topic", event);
                logger.info("Payment completed for order: {}", payment.getOrderNumber());
            } else {
                payment.markAsFailed("Payment gateway declined the transaction");
                logger.warn("Payment failed for order: {}", payment.getOrderNumber());
            }

            paymentRepository.save(payment);
        } catch (Exception e) {
            payment.markAsFailed("Payment processing error: " + e.getMessage());
            paymentRepository.save(payment);
            logger.error("Error processing payment: ", e);
        }
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(payment.getPaymentId());
        response.setOrderNumber(payment.getOrderNumber());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}