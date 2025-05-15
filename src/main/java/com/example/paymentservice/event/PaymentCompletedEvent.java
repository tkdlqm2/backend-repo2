package com.example.paymentservice.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentCompletedEvent {

    private String paymentId;
    private String orderNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime completedAt;

    // 생성자, 게터, 세터
    public PaymentCompletedEvent() {
    }

    public PaymentCompletedEvent(String paymentId, String orderNumber, BigDecimal amount, String status, LocalDateTime completedAt) {
        this.paymentId = paymentId;
        this.orderNumber = orderNumber;
        this.amount = amount;
        this.status = status;
        this.completedAt = completedAt;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}