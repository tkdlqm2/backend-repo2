package com.example.paymentservice.dto;

import java.math.BigDecimal;

public class PaymentRequest {

    private String orderNumber;
    private BigDecimal amount;
    private String paymentMethod;

    // 생성자, 게터, 세터
    public PaymentRequest() {
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}