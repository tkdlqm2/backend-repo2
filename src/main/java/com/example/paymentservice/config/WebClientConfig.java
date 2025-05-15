package com.example.paymentservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient paymentGatewayClient(WebClient.Builder builder) {
        // 실제 프로젝트에서는 외부 결제 게이트웨이 URL 설정 필요
        return builder
                .baseUrl("https://api.payment-gateway.example.com")
                .build();
    }
}