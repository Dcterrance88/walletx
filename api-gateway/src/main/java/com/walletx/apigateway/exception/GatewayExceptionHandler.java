package com.walletx.apigateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.walletx.apigateway.utils.constant.Constants.ErrorResponse.*;
import static com.walletx.apigateway.utils.constant.Constants.Security.GATEWAY_EXCEPTION_HANDLER_ORDER;

/**
 * Global exception handler for unhandled errors in the WalletX API Gateway.
 *
 * <p>Acts as a safety net for any exception that escapes the normal filter chain.
 * Since the Gateway runs on Spring WebFlux, standard Spring MVC mechanisms like
 * {@code @RestControllerAdvice} are not applicable here — this handler implements
 * {@link ErrorWebExceptionHandler} which is the WebFlux equivalent.</p>
 *
 * <p>Runs at order {@code -2} to execute before Spring's default WebFlux error
 * handler, ensuring all unhandled exceptions return a structured JSON response
 * consistent with the rest of the WalletX error format.</p>
 */
@Slf4j
@Order(GATEWAY_EXCEPTION_HANDLER_ORDER)
@Component
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    /**
     * Jackson mapper configured with {@link JavaTimeModule} to support
     * serialization of {@link java.time.LocalDateTime} in the error response.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Handles any unhandled exception that propagates to the Gateway error layer.
     *
     * <p>Builds a structured JSON error response with the following fields:</p>
     * <ul>
     *   <li>{@code code} — the HTTP status code string</li>
     *   <li>{@code message} — the raw exception message for internal tracing</li>
     *   <li>{@code userMessage} — a generic user-facing message</li>
     *   <li>{@code path} — the request path where the error occurred</li>
     *   <li>{@code timestamp} — the date and time when the error was handled</li>
     * </ul>
     *
     * <p>If JSON serialization itself fails, the response is completed empty
     * to avoid leaving the connection hanging.</p>
     *
     * @param exchange  the current server exchange containing request and response
     * @param exception the unhandled exception that triggered this handler
     * @return a {@link Mono} that completes after writing the error response
     */
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("Unhandled gateway exception: path={}, error={}",
                exchange.getRequest().getURI().getPath(),
                exception.getMessage());

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(FIELD_CODE, status.toString());
        errorResponse.put(FIELD_MESSAGE, exception.getMessage());
        errorResponse.put(FIELD_USER_MESSAGE, UNEXPECTED_ERROR);
        errorResponse.put(FIELD_PATH, exchange.getRequest().getURI().getPath());
        errorResponse.put(FIELD_TIMESTAMP, LocalDateTime.now().toString());

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException processingException) {
            log.error("Error serializing error response: {}", processingException.getMessage());
            return exchange.getResponse().setComplete();
        }
    }

}
