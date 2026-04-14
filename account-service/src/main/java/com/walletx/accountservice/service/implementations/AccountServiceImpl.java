package com.walletx.accountservice.service.implementations;

import com.walletx.accountservice.domain.dto.request.CreateAccountRequest;
import com.walletx.accountservice.domain.dto.request.DepositRequest;
import com.walletx.accountservice.domain.dto.request.UpdateAccountStatusRequest;
import com.walletx.accountservice.domain.dto.response.AccountResponse;
import com.walletx.accountservice.domain.entity.Account;
import com.walletx.accountservice.domain.mapper.AccountMapper;
import com.walletx.accountservice.repository.AccountRepository;
import com.walletx.accountservice.rules.AccountRules;
import com.walletx.accountservice.service.interfaces.AccountService;
import com.walletx.accountservice.utils.generator.AccountNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountRules accountRules;
    private final AccountMapper accountMapper;
    private final AccountNumberGenerator accountNumberGenerator;

    @Override
    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        log.info("Create account request received for userId: {}", userId);

        accountRules.validateAccountDoesNotExist(userId);

        Account account = Account.builder()
                .userId(userId)
                .currency(request.getCurrency())
                .accountNumber(accountNumberGenerator.generate(accountRepository::existsByAccountNumber))
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created successfully: {}", saved.getAccountNumber());

        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(Long userId) {
        Account account = accountRules.findByUserIdOrThrow(userId);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse updateMyStatus(Long userId, UpdateAccountStatusRequest request) {
        Account account = accountRules.findByUserIdOrThrow(userId);
        accountRules.validateStatusTransition(account, request.getStatus());

        account.setStatus(request.getStatus());
        Account saved = accountRepository.save(account);
        log.info("Status updated to {} for account: {}", saved.getStatus(), saved.getAccountNumber());

        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AccountResponse deposit(Long userId, DepositRequest request) {
        accountRules.validateDepositAmount(request.getAmount());

        Account account = accountRules.findByUserIdOrThrow(userId);
        accountRules.validateAccountIsActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        Account saved = accountRepository.save(account);
        log.info("Deposit of {} completed for account: {}", request.getAmount(), saved.getAccountNumber());

        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long accountId) {
        Account account = accountRules.findByIdOrThrow(accountId);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse updateAccountStatus(Long accountId, UpdateAccountStatusRequest request) {
        Account account = accountRules.findByIdOrThrow(accountId);
        accountRules.validateStatusTransition(account, request.getStatus());

        account.setStatus(request.getStatus());
        Account saved = accountRepository.save(account);
        log.info("Admin updated status to {} for account: {}", saved.getStatus(), saved.getAccountNumber());

        return accountMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        Account account = accountRules.findByAccountNumberOrThrow(accountNumber);
        return accountMapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAccountNumber(String accountNumber) {
        return accountRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    @Transactional
    public AccountResponse updateBalance(String accountNumber, BigDecimal amount) {
        Account account = accountRules.findByAccountNumberOrThrow(accountNumber);
        accountRules.validateAccountIsActive(account);

        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);
        log.info("Balance updated by {} for account: {}", amount, saved.getAccountNumber());

        return accountMapper.toResponse(saved);
    }

}
