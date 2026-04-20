package com.walletx.accountservice.controller;

import com.walletx.accountservice.domain.dto.request.CreateAccountRequest;
import com.walletx.accountservice.domain.dto.request.DepositRequest;
import com.walletx.accountservice.domain.dto.request.UpdateAccountStatusRequest;
import com.walletx.accountservice.domain.dto.response.AccountResponse;
import com.walletx.accountservice.service.interfaces.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    // ================================
    // User endpoints
    // ================================

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateAccountRequest request) {
        log.info("Request to create account for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(userId, request));
    }

    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMyAccount(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(accountService.getMyAccount(userId));
    }

    @PutMapping("/me/status")
    public ResponseEntity<AccountResponse> updateMyStatus(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(accountService.updateMyStatus(userId, request));
    }

    @PostMapping("/me/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody DepositRequest request) {
        log.info("Deposit request for userId: {}", userId);
        return ResponseEntity.ok(accountService.deposit(userId, request));
    }

    // ================================
    // Admin endpoints
    // ================================

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @PutMapping("/{accountId}/status")
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @PathVariable Long accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountId, request));
    }

    // ================================
    // Internal endpoints
    // ================================

    @GetMapping("/internal/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByAccountNumber(accountNumber));
    }

    @GetMapping("/internal/exists/{accountNumber}")
    public ResponseEntity<Boolean> existsByAccountNumber(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.existsByAccountNumber(accountNumber));
    }

    @PutMapping("/internal/{accountNumber}/balance")
    public ResponseEntity<AccountResponse> updateBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(accountService.updateBalance(accountNumber, amount));
    }

}
