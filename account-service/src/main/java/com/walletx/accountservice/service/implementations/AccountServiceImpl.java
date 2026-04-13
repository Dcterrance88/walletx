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

    @Override
    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        log.info("Creating account for userId: {}", userId);

        accountRules.validateAccountDoesNotExist(userId);

        Account account = Account.builder()
                .userId(userId)
                .currency(request.getCurrency())
                .accountNumber(generateAccountNumber())
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

        log.info("Updating status for account: {} to {}", account.getAccountNumber(), request.getStatus());

        account.setStatus(request.getStatus());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse deposit(Long userId, DepositRequest request) {
        accountRules.validateDepositAmount(request.getAmount());

        Account account = accountRules.findByUserIdOrThrow(userId);
        accountRules.validateAccountIsActive(account);

        log.info("Depositing {} to account: {}", request.getAmount(), account.getAccountNumber());

        account.setBalance(account.getBalance().add(request.getAmount()));
        return accountMapper.toResponse(accountRepository.save(account));
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

        log.info("Admin updating status for account: {} to {}", account.getAccountNumber(), request.getStatus());

        account.setStatus(request.getStatus());
        return accountMapper.toResponse(accountRepository.save(account));
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

        log.info("Updating balance for account: {} by amount: {}", accountNumber, amount);

        account.setBalance(account.getBalance().add(amount));
        return accountMapper.toResponse(accountRepository.save(account));
    }

    private String generateAccountNumber() {
        String accountNumber;
        do {
            long number = (long) (Math.random() * 9_000_000L) + 1_000_000L;
            accountNumber = "WLT-" + number;
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

}
