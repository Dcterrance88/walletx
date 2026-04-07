package com.walletx.authservice.controller;
import com.walletx.authservice.domain.dto.request.LoginRequest;
import com.walletx.authservice.domain.dto.request.RefreshTokenRequest;
import com.walletx.authservice.domain.dto.request.RegisterRequest;
import com.walletx.authservice.domain.dto.response.AuthResponse;
import com.walletx.authservice.domain.dto.response.UserResponse;
import com.walletx.authservice.service.interfaces.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication and user profile operations.
 *
 * <p>Exposes public endpoints for registration, login, token refresh,
 * and logout. Profile retrieval requires a valid JWT token.</p>
 *
 * @see AuthService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user and returns a token pair.
     *
     * @param request the registration request body
     * @return 201 Created with the generated token pair and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates a user and returns a token pair.
     *
     * @param request the login request body
     * @return 200 OK with the generated token pair and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Issues a new access token using a valid refresh token.
     *
     * @param request the refresh token request body
     * @return 200 OK with the new access token and user info
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    /**
     * Logs out the authenticated user by revoking all active refresh tokens.
     *
     * @param userDetails the authenticated user injected by Spring Security
     * @return 204 No Content on successful logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logout(userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the profile of the authenticated user.
     *
     * @param userDetails the authenticated user injected by Spring Security
     * @return 200 OK with the user profile
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getProfile(userDetails.getUsername()));
    }

}
