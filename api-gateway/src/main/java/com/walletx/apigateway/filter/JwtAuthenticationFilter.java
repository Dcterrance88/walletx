package com.walletx.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

import static com.walletx.apigateway.utils.constant.Constants.PublicPaths.*;
import static com.walletx.apigateway.utils.constant.Constants.Security.*;

/**
 * Global filter that enforces JWT authentication on all incoming requests
 * to the WalletX API Gateway.
 *
 * <p>This filter intercepts every request before it reaches any downstream
 * microservice. Public paths (login, register, refresh-token) are allowed
 * through without validation. All other paths require a valid Bearer token.</p>
 *
 * <p>On successful validation, the authenticated user's ID is extracted from
 * the token claims and propagated downstream as the {@code X-User-Id} header,
 * allowing microservices to identify the caller without handling JWT directly.</p>
 *
 * <p>This filter runs before all other Gateway filters ({@code order = -1}).</p>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    /**
     * Base64-encoded secret key used to verify JWT token signatures.
     * Must match the secret used by {@code auth-service} to sign tokens.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Intercepts incoming requests and enforces JWT authentication.
     *
     * <p>The filter follows this sequence:</p>
     * <ol>
     *   <li>Allow public paths through without validation</li>
     *   <li>Extract and validate the Bearer token from the Authorization header</li>
     *   <li>Extract the {@code userId} claim from the token payload</li>
     *   <li>Propagate the userId as {@code X-User-Id} header to downstream services</li>
     * </ol>
     *
     * @param exchange the current server exchange containing request and response
     * @param chain    the filter chain to delegate to the next filter
     * @return a {@link Mono} that completes when the filter chain finishes,
     *         or immediately with {@code 401} if authentication fails
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange, path);
        if (token == null) {
            return unauthorized(exchange);
        }

        Long userId = extractUserId(token, path);
        if (userId == null) {
            return unauthorized(exchange);
        }

        return chain.filter(propagateUserId(exchange, userId));
    }

    /**
     * Extracts the JWT token from the {@code Authorization} header.
     *
     * <p>Expects the header to follow the format {@code Bearer <token>}.
     * Returns {@code null} if the header is absent or malformed.</p>
     *
     * @param exchange the current server exchange
     * @param path     the request path, used for logging purposes
     * @return the raw JWT token string, or {@code null} if not present or invalid
     */
    private String extractToken(ServerWebExchange exchange, String path) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or invalid Authorization header: path={}", path);
            return null;
        }

        return authHeader.substring(BEARER_PREFIX.length());
    }

    /**
     * Validates the JWT token and extracts the {@code userId} claim from its payload.
     *
     * <p>Returns {@code null} if the token is expired, has an invalid signature,
     * is malformed, or does not contain the {@code userId} claim.</p>
     *
     * @param token the raw JWT token string
     * @param path  the request path, used for logging purposes
     * @return the authenticated user's ID, or {@code null} if validation fails
     */
    private Long extractUserId(String token, String path) {
        try {
            Claims claims = extractClaims(token);
            Long userId = claims.get("userId", Long.class);

            if (userId == null) {
                log.warn("JWT does not contain userId claim: path={}", path);
            }

            return userId;

        } catch (ExpiredJwtException exception) {
            log.warn("Expired JWT token: path={}", path);
        } catch (SignatureException | MalformedJwtException exception) {
            log.warn("Invalid JWT token: path={}", path);
        } catch (Exception exception) {
            log.error("Unexpected error validating JWT: path={}, error={}", path, exception.getMessage());
        }

        return null;
    }

    /**
     * Creates a mutated copy of the exchange with the {@code X-User-Id} header added.
     *
     * <p>Since {@link ServerWebExchange} is immutable, this method uses
     * {@code mutate()} to produce a new instance with the additional header,
     * leaving the original exchange unchanged.</p>
     *
     * @param exchange the original server exchange
     * @param userId   the authenticated user's ID to propagate
     * @return a new {@link ServerWebExchange} with the {@code X-User-Id} header set
     */
    private ServerWebExchange propagateUserId(ServerWebExchange exchange, Long userId) {
        return exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header(X_USER_ID_HEADER, String.valueOf(userId))
                        .build())
                .build();
    }

    /**
     * Terminates the request with a {@code 401 Unauthorized} response.
     *
     * @param exchange the current server exchange
     * @return a {@link Mono} that completes after writing the 401 response
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    /**
     * Determines whether the given path is publicly accessible without authentication.
     *
     * @param path the request path to evaluate
     * @return {@code true} if the path is public, {@code false} otherwise
     */
    private boolean isPublicPath(String path) {
        return path.startsWith(AUTH_LOGIN) ||
                path.startsWith(AUTH_REGISTER) ||
                path.startsWith(AUTH_REFRESH_TOKEN);
    }

    /**
     * Parses the JWT token and returns all claims from its payload.
     *
     * @param token the raw JWT token string
     * @return the {@link Claims} payload extracted from the token
     */
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Decodes the Base64-encoded secret and builds the HMAC-SHA signing key.
     *
     * @return the {@link SecretKey} used to verify JWT token signatures
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Returns the execution order of this filter in the Gateway filter chain.
     *
     * <p>A value of {@code -1} ensures this filter runs before all default
     * Spring Cloud Gateway filters.</p>
     *
     * @return the filter order
     */
    @Override
    public int getOrder() {
        return JWT_FILTER_ORDER;
    }

}
