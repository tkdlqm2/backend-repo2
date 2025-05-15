// src/main/java/com/example/paymentservice/event/OrderCreatedEvent.java
package com.example.paymentservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderCreatedEvent {

    private String orderNumber;
    private String customerEmail;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private String paymentMethod;  // 결제 방법 추가

    // 기본 생성자 (JSON 변환을 위해 필요)
    public OrderCreatedEvent() {
    }

    public OrderCreatedEvent(String orderNumber, String customerEmail, BigDecimal totalAmount,
                             LocalDateTime createdAt, String paymentMethod) {
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.paymentMethod = paymentMethod;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String toString() {
        return "OrderCreatedEvent{" +
                "orderNumber='" + orderNumber + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", totalAmount=" + totalAmount +
                ", createdAt=" + createdAt +
                ", paymentMethod='" + paymentMethod + '\'' +
                '}';
    }
}