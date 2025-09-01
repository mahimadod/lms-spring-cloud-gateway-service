package com.example.gateway.service.filters;

import com.example.gateway.service.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Component
public class LoggingGatewayFilterFactory extends
        AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {

    final Logger logger =
      LoggerFactory.getLogger(LoggingGatewayFilterFactory.class);

    @Autowired
    private WebClientConfig webClient;


    private final WebClient.Builder webClientBuilder;

    public LoggingGatewayFilterFactory(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            // Pre-processing
            if (config.isPreLogger()) {
                logger.info("Pre GatewayFilter logging-"
                        + config.getBaseMessage());
            }

            String token=exchange.getRequest().getHeaders().containsKey("token")? Objects.requireNonNull(exchange.getRequest().getHeaders().get("token")).get(0):"";
            return sendJWTTokenValidationRequest(token)
                    .flatMap(valid -> {
                        if (!valid) {
                            logger.warn("Token validation failed");
                            return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
                        }

                        logger.info("Token validated successfully");
                        return chain.filter(exchange)
                                .then(Mono.fromRunnable(() -> {
                                    if (config.isPostLogger()) {
                                        logger.info("Post GatewayFilter logging: " + config.getBaseMessage());
                                    }
                                }));
                    });
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

    private Mono<Boolean> sendJWTTokenValidationRequest(String token) {
        if (token == null || token.isEmpty()) {
            logger.warn("Token is missing or empty");
            return Mono.just(false);
        }

        String uri = "/auth-service/validate?token=" + token;
        logger.debug("Calling Auth Service: {}", uri);
        try {
            return webClientBuilder.baseUrl("lb://spring-cloud-gateway-service/").build()
                    .get()
                    .uri("auth-service/validate?token=" + token)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> {
                        logger.warn("Received error status from Auth Service: {}", response.statusCode());
                        return Mono.error(new RuntimeException("Invalid response from Auth Service"));
                    })
                    .bodyToMono(AuthResponse.class)
                    .doOnNext(authResponse -> logger.debug("Auth Response: {}", authResponse))
                    .map(AuthResponse::isValidToken)
                    .onErrorResume(e -> {
                        logger.error("Error calling Auth Service", e);
                        return Mono.just(false);
                    });
        }catch(Exception e){
            logger.error("Exception occurred while sending JWT token validation request", e);
            return Mono.just(false);
        }
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
