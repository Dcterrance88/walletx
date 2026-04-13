package com.walletx.accountservice.rules;

import com.walletx.accountservice.domain.entity.Account;
import com.walletx.accountservice.domain.enums.AccountStatus;
import com.walletx.common.exception.WalletXException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.walletx.accountservice.repository.AccountRepository;

import static com.walletx.accountservice.utils.constant.Constants.MessageKeys.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountRules {

    private final AccountRepository accountRepository;

    // ================================
    // Finders
    // ================================

    public Account findByAccountNumberOrThrow(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new WalletXException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
    }

    public Account findByUserIdOrThrow(Long userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletXException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
    }

    public Account findByIdOrThrow(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new WalletXException(HttpStatus.NOT_FOUND, ACCOUNT_NOT_FOUND));
    }

    // ================================
    // Business validations
    // ================================

    public void validateAccountDoesNotExist(Long userId) {
        if (accountRepository.existsByUserId(userId)) {
            throw new WalletXException(HttpStatus.CONFLICT, ACCOUNT_ALREADY_EXISTS);
        }
    }

    public void validateAccountIsActive(Account account) {
        if (account.getStatus() == AccountStatus.FROZEN) {
            throw new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, ACCOUNT_FROZEN);
        }
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, ACCOUNT_CLOSED);
        }
    }

    public void validateSufficientFunds(Account account, java.math.BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, ACCOUNT_INSUFFICIENT_FUNDS);
        }
    }

    public void validateDepositAmount(java.math.BigDecimal amount) {
        if (amount == null || amount.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new WalletXException(HttpStatus.BAD_REQUEST, ACCOUNT_DEPOSIT_INVALID_AMOUNT);
        }
    }

    public void validateStatusTransition(Account account, AccountStatus newStatus) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, ACCOUNT_INVALID_STATUS_TRANSITION);
        }
    }

}
