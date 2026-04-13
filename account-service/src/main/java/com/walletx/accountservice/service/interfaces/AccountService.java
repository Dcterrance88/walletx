package com.walletx.accountservice.service.interfaces;

import com.walletx.accountservice.domain.dto.request.CreateAccountRequest;
import com.walletx.accountservice.domain.dto.request.DepositRequest;
import com.walletx.accountservice.domain.dto.request.UpdateAccountStatusRequest;
import com.walletx.accountservice.domain.dto.response.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse createAccount(Long userId, CreateAccountRequest request);

    AccountResponse getMyAccount(Long userId);

    AccountResponse updateMyStatus(Long userId, UpdateAccountStatusRequest request);

    AccountResponse deposit(Long userId, DepositRequest request);

    // Admin
    List<AccountResponse> getAllAccounts();

    AccountResponse getAccountById(Long accountId);

    AccountResponse updateAccountStatus(Long accountId, UpdateAccountStatusRequest request);

    // Internal
    AccountResponse getAccountByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    AccountResponse updateBalance(String accountNumber, java.math.BigDecimal amount);

}
