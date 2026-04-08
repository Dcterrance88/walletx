package com.walletx.authservice.service.implementations;

import com.walletx.authservice.domain.dto.request.LoginRequest;
import com.walletx.authservice.domain.dto.request.RefreshTokenRequest;
import com.walletx.authservice.domain.dto.request.RegisterRequest;
import com.walletx.authservice.domain.dto.response.AuthResponse;
import com.walletx.authservice.domain.dto.response.UserResponse;
import com.walletx.authservice.domain.entity.RefreshToken;
import com.walletx.authservice.domain.entity.Role;
import com.walletx.authservice.domain.entity.User;
import com.walletx.authservice.domain.mapper.UserMapper;
import com.walletx.authservice.repository.RefreshTokenRepository;
import com.walletx.authservice.repository.UserRepository;
import com.walletx.authservice.rules.RefreshTokenRules;
import com.walletx.authservice.rules.UserRules;
import com.walletx.authservice.security.JwtTokenService;
import com.walletx.authservice.service.interfaces.AuthService;
import com.walletx.authservice.utils.constant.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;
    private final UserRules userRules;
    private final RefreshTokenRules refreshTokenRules;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        userRules.validateEmailIsNotRegistered(request.getEmail());

        Role defaultRole = userRules.findDefaultRoleOrFail();

        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(defaultRole))
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("User registered successfully with id: {}", savedUser.getId());

        return buildAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        User user = userRules.findUserByEmailOrFail(userDetails.getUsername());

        userRules.validateAccountIsActive(user.getIsActive());

        revokeAllUserRefreshTokens(user);

        log.info("User logged in successfully: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Refresh token request received");

        RefreshToken refreshToken = refreshTokenRules.findByTokenOrFail(request.getRefreshToken());

        refreshTokenRules.validateTokenIsNotRevoked(refreshToken);
        refreshTokenRules.validateTokenIsNotExpired(refreshToken);

        User user = refreshToken.getUser();
        userRules.validateAccountIsActive(user.getIsActive());

        revokeAllUserRefreshTokens(user);

        log.info("Refresh token issued successfully for user: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(String email) {
        log.info("Logout request for user: {}", email);

        User user = userRules.findUserByEmailOrFail(email);

        revokeAllUserRefreshTokens(user);

        log.info("User logged out successfully: {}", email);
    }

    @Override
    public UserResponse getProfile(String email) {
        log.info("Profile request for user: {}", email);

        User user = userRules.findUserByEmailOrFail(email);

        return userMapper.toUserResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .toArray(String[]::new))
                .build();

        String accessToken = jwtTokenService.generateAccessToken(userDetails);
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType(Constants.Security.TOKEN_TYPE)
                .expiresIn(refreshExpiration)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpiration / 1000))
                .build();

        return refreshTokenRepository.save(newRefreshToken);
    }

    private void revokeAllUserRefreshTokens(User user) {
        refreshTokenRepository.findAllByUserAndIsRevokedFalse(user)
                .forEach(token -> {
                    token.setIsRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

}
