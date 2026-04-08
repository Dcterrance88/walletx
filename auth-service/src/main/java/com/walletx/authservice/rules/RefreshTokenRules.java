package com.walletx.authservice.rules;
import com.walletx.authservice.domain.entity.RefreshToken;
import com.walletx.authservice.exception.WalletXException;
import com.walletx.authservice.repository.RefreshTokenRepository;
import com.walletx.authservice.utils.constant.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenRules {

    private final RefreshTokenRepository refreshTokenRepository;

    // ── Finders ──────────────────────────────────────────────────────────────

    public RefreshToken findByTokenOrFail(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new WalletXException(
                        HttpStatus.UNAUTHORIZED,
                        Constants.MessageKeys.INVALID_TOKEN
                ));
    }

    // ── Validations ───────────────────────────────────────────────────────────

    public void validateTokenIsNotRevoked(RefreshToken refreshToken) {
        if (Boolean.TRUE.equals(refreshToken.getIsRevoked())) {
            log.warn("Attempt to use revoked refresh token");
            throw new WalletXException(HttpStatus.UNAUTHORIZED, Constants.MessageKeys.TOKEN_REVOKED);
        }
    }

    public void validateTokenIsNotExpired(RefreshToken refreshToken) {
        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Attempt to use expired refresh token");
            throw new WalletXException(HttpStatus.UNAUTHORIZED, Constants.MessageKeys.TOKEN_EXPIRED);
        }
    }

}
