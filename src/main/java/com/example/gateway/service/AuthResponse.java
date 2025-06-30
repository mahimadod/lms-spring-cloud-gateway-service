package com.example.gateway.service;

import lombok.*;
import org.springframework.web.service.annotation.GetExchange;

@Builder
@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
        private String token;
        private boolean validToken;
        private String error;
    }
