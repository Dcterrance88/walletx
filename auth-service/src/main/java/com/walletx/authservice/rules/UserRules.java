package com.walletx.authservice.rules;

import com.walletx.authservice.domain.entity.Role;
import com.walletx.authservice.domain.entity.User;
import com.walletx.authservice.domain.enums.RoleType;
import com.walletx.common.exception.WalletXException;
import com.walletx.authservice.repository.RoleRepository;
import com.walletx.authservice.repository.UserRepository;
import com.walletx.authservice.utils.constant.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRules {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    // ── Finders ──────────────────────────────────────────────────────────────

    public User findUserByEmailOrFail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new WalletXException(
                        HttpStatus.UNAUTHORIZED,
                        Constants.MessageKeys.USER_NOT_FOUND
                ));
    }

    public Role findDefaultRoleOrFail() {
        return roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new WalletXException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        Constants.MessageKeys.ROLE_NOT_FOUND
                ));
    }

    // ── Validations ───────────────────────────────────────────────────────────

    public void validateEmailIsNotRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration attempt with already registered email: {}", email);
            throw new WalletXException(HttpStatus.CONFLICT, Constants.MessageKeys.USER_ALREADY_EXISTS);
        }
    }

    public void validateAccountIsActive(Boolean isActive) {
        if (Boolean.FALSE.equals(isActive)) {
            log.warn("Operation attempted on a disabled account");
            throw new WalletXException(HttpStatus.FORBIDDEN, Constants.MessageKeys.ACCOUNT_DISABLED);
        }
    }

}
