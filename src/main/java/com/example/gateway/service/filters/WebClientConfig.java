package com.example.gateway.service.filters;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient getAuthenticatorWebClient() {
        return WebClient.builder()
                .baseUrl("http://LMS-AUTHENTICATOR-SERVICE") // Change if needed
                .build();
    }
}


