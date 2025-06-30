package com.example.gateway.service.filters;

import com.example.gateway.service.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;

@Component
public class AuthGatewayFilterFactory extends
        AbstractGatewayFilterFactory<AuthGatewayFilterFactory.Config> {

    final Logger logger =
      LoggerFactory.getLogger(AuthGatewayFilterFactory.class);

    public AuthGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            // Pre-processing
            if (config.isPreLogger()) {
                logger.info("Pre GatewayFilter logging: "
                        + config.getBaseMessage());
            }
                return chain.filter(exchange)
                        .then(Mono.fromRunnable(() -> {
                            // Post-processing
                            if (config.isPostLogger()) {
                                logger.info("Post GatewayFilter logging: "
                                        + config.getBaseMessage());
                            }
                        }));
        });
    }
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
                status.getReasonPhrase(), message, status.value(), Instant.now().toString()
        );

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean sendJWTTokenValidationRequest(String token) {
        if(!token.isEmpty()){
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<AuthResponse> response=restTemplate.exchange("http://localhost:8091/auth-service/validate?token="+token, HttpMethod.GET,null, AuthResponse.class);
            if(response.getStatusCode().is2xxSuccessful()){
                return Objects.requireNonNull(response.getBody()).isValidToken();
            }else {
                return false;
            }
        }
        return false;
    }

    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;

        public Config(String baseMessage, boolean preLogger, boolean postLogger) {
            this.baseMessage = baseMessage;
            this.preLogger = preLogger;
            this.postLogger = postLogger;
        }

        public String getBaseMessage() {
            return baseMessage;
        }

        public void setBaseMessage(String baseMessage) {
            this.baseMessage = baseMessage;
        }

        public boolean isPreLogger() {
            return preLogger;
        }

        public void setPreLogger(boolean preLogger) {
            this.preLogger = preLogger;
        }

        public boolean isPostLogger() {
            return postLogger;
        }

        public void setPostLogger(boolean postLogger) {
            this.postLogger = postLogger;
        }
    }

}
