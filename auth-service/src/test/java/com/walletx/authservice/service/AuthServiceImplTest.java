package com.walletx.authservice.service;

import com.walletx.authservice.domain.dto.request.LoginRequest;
import com.walletx.authservice.domain.dto.request.RefreshTokenRequest;
import com.walletx.authservice.domain.dto.request.RegisterRequest;
import com.walletx.authservice.domain.dto.response.AuthResponse;
import com.walletx.authservice.domain.dto.response.UserResponse;
import com.walletx.authservice.domain.entity.RefreshToken;
import com.walletx.authservice.domain.entity.Role;
import com.walletx.authservice.domain.entity.User;
import com.walletx.authservice.domain.enums.RoleType;
import com.walletx.authservice.domain.mapper.UserMapper;
import com.walletx.authservice.exception.WalletXException;
import com.walletx.authservice.repository.RefreshTokenRepository;
import com.walletx.authservice.repository.UserRepository;
import com.walletx.authservice.rules.RefreshTokenRules;
import com.walletx.authservice.rules.UserRules;
import com.walletx.authservice.security.JwtTokenService;
import com.walletx.authservice.service.implementations.AuthServiceImpl;
import com.walletx.authservice.utils.constant.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private UserRules userRules;
    @Mock private RefreshTokenRules refreshTokenRules;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenService jwtTokenService;
    @Mock private UserMapper userMapper;
    @Mock private Authentication authentication;

    private RegisterRequest registerRequest;
    private User savedUser;
    private Role defaultRole;
    private UserResponse userResponse;
    private RefreshToken savedRefreshToken;
    private UserDetails userDetails;
    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private RefreshToken existingRefreshToken;
    private String userEmail;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);

        defaultRole = Role.builder()
                .id(1L)
                .name(RoleType.ROLE_USER)
                .build();

        loginRequest = LoginRequest.builder()
                .email("john.doe@example.com")
                .password("Password123!")
                .build();

        registerRequest = RegisterRequest.builder()
                .email("john.doe@example.com")
                .password("Password123!")
                .fullName("John Doe")
                .phoneNumber("+573001234567")
                .build();

        savedUser = User.builder()
                .id(1L)
                .email("john.doe@example.com")
                .password("$2a$10$hashedpassword")
                .fullName("John Doe")
                .phoneNumber("+573001234567")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .roles(Set.of(defaultRole))
                .build();

        userResponse = UserResponse.builder()
                .id(1L)
                .email("john.doe@example.com")
                .fullName("John Doe")
                .phoneNumber("+573001234567")
                .isActive(true)
                .roles(Set.of("ROLE_USER"))
                .createdAt(savedUser.getCreatedAt())
                .build();

        savedRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("test-refresh-token-uuid")
                .user(savedUser)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().plusSeconds(604800000L / 1000))
                .build();

        userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("john.doe@example.com")
                .password("$2a$10$hashedpassword")
                .authorities("ROLE_USER")
                .build();

        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("test-refresh-token-uuid")
                .build();

        existingRefreshToken = RefreshToken.builder()
                .id(2L)
                .token("test-refresh-token-uuid")
                .user(savedUser)
                .isRevoked(false)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        userEmail = "john.doe@example.com";
    }

    @Test
    void register_WhenValidRequest_ShouldReturnAuthResponse() {
        when(userRules.findDefaultRoleOrFail()).thenReturn(defaultRole);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenService.generateAccessToken(any())).thenReturn("mocked-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefreshToken);
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("mocked-access-token", result.getAccessToken());
        assertEquals("test-refresh-token-uuid", result.getRefreshToken());
        assertEquals(Constants.Security.TOKEN_TYPE, result.getTokenType());
        assertEquals(604800000L, result.getExpiresIn());
        assertEquals(userResponse, result.getUser());

        verify(userRules).validateEmailIsNotRegistered(registerRequest.getEmail());
        verify(userRules).findDefaultRoleOrFail();
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtTokenService).generateAccessToken(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userMapper).toUserResponse(savedUser);
    }

    @Test
    void register_WhenEmailAlreadyExists_ShouldThrowWalletXException() {
        doThrow(new WalletXException(HttpStatus.CONFLICT, Constants.MessageKeys.USER_ALREADY_EXISTS))
                .when(userRules).validateEmailIsNotRegistered(registerRequest.getEmail());

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> authService.register(registerRequest)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_WhenValidRequest_ShouldReturnAuthResponse() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRules.findUserByEmailOrFail(userDetails.getUsername())).thenReturn(savedUser);
        when(refreshTokenRepository.findAllByUserAndIsRevokedFalse(savedUser)).thenReturn(List.of());
        when(jwtTokenService.generateAccessToken(any())).thenReturn("mocked-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefreshToken);
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("mocked-access-token", result.getAccessToken());
        assertEquals("test-refresh-token-uuid", result.getRefreshToken());
        assertEquals(Constants.Security.TOKEN_TYPE, result.getTokenType());
        assertEquals(604800000L, result.getExpiresIn());
        assertEquals(userResponse, result.getUser());

        verify(authenticationManager).authenticate(any());
        verify(authentication).getPrincipal();
        verify(userRules).findUserByEmailOrFail(userDetails.getUsername());
        verify(userRules).validateAccountIsActive(savedUser.getIsActive());
        verify(refreshTokenRepository).findAllByUserAndIsRevokedFalse(savedUser);
        verify(jwtTokenService).generateAccessToken(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userMapper).toUserResponse(savedUser);
    }

    @Test
    void login_WhenUserIsNotActive_ShouldThrowWalletXException() {
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRules.findUserByEmailOrFail(userDetails.getUsername())).thenReturn(savedUser);
        doThrow(new WalletXException(HttpStatus.FORBIDDEN, Constants.MessageKeys.ACCOUNT_DISABLED))
                .when(userRules).validateAccountIsActive(savedUser.getIsActive());

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(userRules).validateAccountIsActive(savedUser.getIsActive());
        verify(refreshTokenRepository, never()).findAllByUserAndIsRevokedFalse(any());
        verify(jwtTokenService, never()).generateAccessToken(any());
    }

    @Test
    void login_WhenWrongCredentials_ShouldThrowBadCredentialsException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("Bad credentials", exception.getMessage());
        verify(authenticationManager).authenticate(any());
        verify(authentication, never()).getPrincipal();
        verify(userRules, never()).findUserByEmailOrFail(any());
        verify(userRules, never()).validateAccountIsActive(any());
        verify(refreshTokenRepository, never()).findAllByUserAndIsRevokedFalse(any());
        verify(jwtTokenService, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(userMapper, never()).toUserResponse(any());
    }

    @Test
    void refreshToken_WhenValidRequest_ShouldReturnAuthResponse() {
        when(refreshTokenRules.findByTokenOrFail(refreshTokenRequest.getRefreshToken())).thenReturn(existingRefreshToken);
        when(refreshTokenRepository.findAllByUserAndIsRevokedFalse(existingRefreshToken.getUser())).thenReturn(List.of());
        when(jwtTokenService.generateAccessToken(any())).thenReturn("mocked-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedRefreshToken);
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        AuthResponse result = authService.refreshToken(refreshTokenRequest);

        assertNotNull(result);
        assertEquals("mocked-access-token", result.getAccessToken());
        assertEquals("test-refresh-token-uuid", result.getRefreshToken());
        assertEquals(Constants.Security.TOKEN_TYPE, result.getTokenType());
        assertEquals(604800000L, result.getExpiresIn());
        assertEquals(userResponse, result.getUser());

        verify(refreshTokenRules).findByTokenOrFail(refreshTokenRequest.getRefreshToken());
        verify(refreshTokenRules).validateTokenIsNotRevoked(existingRefreshToken);
        verify(refreshTokenRules).validateTokenIsNotExpired(existingRefreshToken);
        verify(userRules).validateAccountIsActive(savedUser.getIsActive());
        verify(refreshTokenRepository).findAllByUserAndIsRevokedFalse(existingRefreshToken.getUser());
        verify(jwtTokenService).generateAccessToken(any());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        verify(userMapper).toUserResponse(savedUser);
    }

    @Test
    void logout_WhenValidEmail_ShouldRevokeAllTokens() {
        when(userRules.findUserByEmailOrFail(userEmail)).thenReturn(savedUser);
        when(refreshTokenRepository.findAllByUserAndIsRevokedFalse(savedUser)).thenReturn(List.of(existingRefreshToken));

        authService.logout(userEmail);

        verify(userRules).findUserByEmailOrFail(userEmail);
        verify(refreshTokenRepository).findAllByUserAndIsRevokedFalse(savedUser);
        verify(refreshTokenRepository).save(existingRefreshToken);
    }

    @Test
    void getProfile_WhenValidEmail_ShouldReturnUserResponse() {
        when(userRules.findUserByEmailOrFail(userEmail)).thenReturn(savedUser);
        when(userMapper.toUserResponse(savedUser)).thenReturn(userResponse);

        UserResponse result = authService.getProfile(userEmail);

        assertEquals(userResponse, result);
        verify(userRules).findUserByEmailOrFail(userEmail);
        verify(userMapper).toUserResponse(savedUser);
    }

}
