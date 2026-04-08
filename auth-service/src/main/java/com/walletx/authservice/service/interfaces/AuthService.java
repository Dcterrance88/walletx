package com.walletx.authservice.service.interfaces;
import com.walletx.authservice.domain.dto.request.LoginRequest;
import com.walletx.authservice.domain.dto.request.RefreshTokenRequest;
import com.walletx.authservice.domain.dto.request.RegisterRequest;
import com.walletx.authservice.domain.dto.response.AuthResponse;
import com.walletx.authservice.domain.dto.response.UserResponse;

/**
 * Contract for authentication and user management operations
 * within the WalletX platform.
 *
 * <p>Defines the core authentication lifecycle — registration, login,
 * token refresh, logout, and profile retrieval. All implementations
 * must fulfill this contract without exposing infrastructure details
 * to the controller layer.</p>
 *
 * @see com.walletx.authservice.service.implementations.AuthServiceImpl
 */
public interface AuthService {

    /**
     * Registers a new user in the system.
     *
     * <p>Validates that the email is not already registered,
     * encodes the password, assigns the default role, and
     * generates an access and refresh token pair.</p>
     *
     * @param request the registration request containing user details
     * @return an {@link AuthResponse} with the generated token pair and user info
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user with their email and password.
     *
     * <p>Validates credentials, verifies the account is active,
     * and generates a new access and refresh token pair.</p>
     *
     * @param request the login request containing email and password
     * @return an {@link AuthResponse} with the generated token pair and user info
     */
    AuthResponse login(LoginRequest request);

    /**
     * Generates a new access token using a valid refresh token.
     *
     * <p>Validates that the refresh token exists, has not been revoked,
     * and has not expired before issuing a new access token.</p>
     *
     * @param request the refresh token request
     * @return an {@link AuthResponse} with the new access token and user info
     */
    AuthResponse refreshToken(RefreshTokenRequest request);

    /**
     * Logs out the authenticated user by revoking all their active refresh tokens.
     *
     * <p>Invalidates the session on the server side — subsequent requests
     * with the old refresh token will be rejected.</p>
     *
     * @param email the email of the user to log out
     */
    void logout(String email);

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param email the email of the authenticated user
     * @return a {@link UserResponse} with the user's profile information
     */
    UserResponse getProfile(String email);

}
