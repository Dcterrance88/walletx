package com.walletx.authservice.security;

import com.walletx.authservice.config.security.SecurityConfig;
import com.walletx.authservice.utils.constant.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication filter that intercepts every incoming HTTP request.
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee single execution per request.
 * Extracts and validates JWT tokens from the request headers, then sets
 * the authentication inf the {@link SecurityContextHolder} if the token is valid.</p>
 *
 * <p>If no token is present, the request continues unauthenticated - allowing
 * public endpoints to remain accessible without credentials.</p>
 *
 * @see JwtTokenService
 * @see JwtAuthenticationEntryPoint
 * @see SecurityConfig
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    /**
     * Intercepts each HTTP request to validate the JWT token and set authentication.
     *
     * <p>The filter follows this process:</p>
     * <ol>
     *     <li>Extracts the JWT token from the Authorization header</li>
     *     <li>If no token is found, continues the filter chain unauthenticated</li>
     *     <li>Validates the token structure and signature</li>
     *     <li>Loads the user from the database using the email in the token</li>
     *     <li>Sets the authentication in the {@link SecurityContextHolder} </li>
     * </ol>
     *
     * <p>If any step fails, the security context is cleared, and the request
     * continues - Spring Security will then invoke {@link JwtAuthenticationEntryPoint}
     * if the requested resource requires authentication.</p>
     *
     * @param request           the incoming HTTP request
     * @param response          the outgoing HTTP response
     * @param filterChain       the filter chain to continue the processing
     * @throws ServletException if the filter chain processing fails
     * @throws IOException      if an I/O error occurs during the processing
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtTokenService.validateTokenStructure(token);

            String email = jwtTokenService.extractUsername(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtTokenService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("Authentication set for user: {}", email);
                }
            }
        } catch (Exception exception) {
            log.error("Authentication filter error: {}", exception.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the Authorization header of the HTTP request.
     *
     * <p>Excepts the header to follow the format: {@code Authorization: Bearer <token>}.
     * Returns {@code null} if the header is absent, empty, or does not start
     * with the expected prefix.</p>
     *
     * @param request the incoming HTTP request
     * @return the extracted JWT token, or {@code null} if not present
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(Constants.Security.AUTH_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(Constants.Security.BEARER_PREFIX)) {
            return bearerToken.substring(Constants.Security.BEARER_PREFIX.length());
        }

        return null;
    }

}
