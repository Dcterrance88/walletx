package com.walletx.authservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.walletx.authservice.config.security.SecurityConfig;
import com.walletx.common.exception.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Entry point for handling unauthorized access attempts in the WalletX application.
 *
 * <p>Invoked by Spring Security when an unauthenticated request attempts to access
 * a protected resource. Unlike {@link com.walletx.common.exception.GlobalExceptionHandler},
 * this component operates at the filter level — before the request reaches the
 * controller layer — so it writes the error response directly to the
 * {@link HttpServletResponse}.</p>
 *
 * @see JwtAuthenticationFilter
 * @see SecurityConfig
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Jackson mapper used to serialize {@link ApiErrorResponse} to JSON.
     * Autoconfigured by Spring Boot and injected via constructor.
     */
    private final ObjectMapper objectMapper;

    /**
     * Handles unauthorized access by returning a structured JSON error response.
     *
     * <p>This method is triggered when a request arrives without valid authentication
     * credentials. It bypasses the standard Spring MVC exception handling and writes
     * the {@link ApiErrorResponse} directly to the response output stream.</p>
     *
     * @param request       the incoming HTTP request that triggered the authentication failure
     * @param response      the HTTP response to write the error to
     * @param authException the exception that caused the authentication failure
     * @throws IOException if writing to the response output stream fails
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt to: {}", request.getRequestURI());

        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .code(HttpStatus.UNAUTHORIZED.toString())
                .userMessage(authException.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

}
