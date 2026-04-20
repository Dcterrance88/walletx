package com.walletx.accountservice.service;

import com.walletx.accountservice.domain.dto.request.CreateAccountRequest;
import com.walletx.accountservice.domain.dto.request.DepositRequest;
import com.walletx.accountservice.domain.dto.request.UpdateAccountStatusRequest;
import com.walletx.accountservice.domain.dto.response.AccountResponse;
import com.walletx.accountservice.domain.entity.Account;
import com.walletx.accountservice.domain.enums.AccountStatus;
import com.walletx.accountservice.domain.enums.AccountType;
import com.walletx.accountservice.domain.enums.Currency;
import com.walletx.accountservice.domain.mapper.AccountMapper;
import com.walletx.accountservice.repository.AccountRepository;
import com.walletx.accountservice.rules.AccountRules;
import com.walletx.accountservice.service.implementations.AccountServiceImpl;
import com.walletx.accountservice.utils.constant.Constants;
import com.walletx.accountservice.utils.generator.AccountNumberGenerator;
import com.walletx.common.exception.WalletXException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @InjectMocks
    private AccountServiceImpl accountService;

    @Mock private AccountRepository accountRepository;
    @Mock private AccountRules accountRules;
    @Mock private AccountMapper accountMapper;
    @Mock private AccountNumberGenerator accountNumberGenerator;

    private Account activeAccount;
    private Account frozenAccount;
    private Account closedAccount;
    private AccountResponse accountResponse;
    private CreateAccountRequest createAccountRequest;
    private DepositRequest depositRequest;
    private UpdateAccountStatusRequest updateStatusRequest;

    private static final Long USER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;
    private static final String ACCOUNT_NUMBER = "WLT-1234567";

    @BeforeEach
    void setUp() {
        activeAccount = Account.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .userId(USER_ID)
                .balance(BigDecimal.valueOf(1000))
                .currency(Currency.COP)
                .accountType(AccountType.WALLET)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        frozenAccount = Account.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .userId(USER_ID)
                .balance(BigDecimal.valueOf(500))
                .currency(Currency.COP)
                .accountType(AccountType.WALLET)
                .status(AccountStatus.FROZEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        closedAccount = Account.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .userId(USER_ID)
                .balance(BigDecimal.ZERO)
                .currency(Currency.COP)
                .accountType(AccountType.WALLET)
                .status(AccountStatus.CLOSED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        accountResponse = AccountResponse.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .userId(USER_ID)
                .balance(BigDecimal.valueOf(1000))
                .currency(Currency.COP)
                .accountType(AccountType.WALLET)
                .status(AccountStatus.ACTIVE)
                .createdAt(activeAccount.getCreatedAt())
                .updatedAt(activeAccount.getUpdatedAt())
                .build();

        createAccountRequest = new CreateAccountRequest();
        createAccountRequest.setCurrency(Currency.COP);

        depositRequest = new DepositRequest();
        depositRequest.setAmount(BigDecimal.valueOf(500));

        updateStatusRequest = new UpdateAccountStatusRequest();
        updateStatusRequest.setStatus(AccountStatus.FROZEN);
    }

    // ================================
    // createAccount
    // ================================

    @Test
    void createAccount_WhenValidRequest_ShouldReturnAccountResponse() {
        when(accountNumberGenerator.generate(any())).thenReturn(ACCOUNT_NUMBER);
        when(accountRepository.save(any(Account.class))).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(accountResponse);

        AccountResponse result = accountService.createAccount(USER_ID, createAccountRequest);

        assertNotNull(result);
        assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(Currency.COP, result.getCurrency());

        verify(accountRules).validateAccountDoesNotExist(USER_ID);
        verify(accountNumberGenerator).generate(any());
        verify(accountRepository).save(any(Account.class));
        verify(accountMapper).toResponse(activeAccount);
    }

    @Test
    void createAccount_WhenAccountAlreadyExists_ShouldThrowWalletXException() {
        doThrow(new WalletXException(HttpStatus.CONFLICT, Constants.MessageKeys.ACCOUNT_ALREADY_EXISTS))
                .when(accountRules).validateAccountDoesNotExist(USER_ID);

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.createAccount(USER_ID, createAccountRequest)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(accountRepository, never()).save(any());
        verify(accountMapper, never()).toResponse(any());
    }

    // ================================
    // getMyAccount
    // ================================

    @Test
    void getMyAccount_WhenAccountExists_ShouldReturnAccountResponse() {
        when(accountRules.findByUserIdOrThrow(USER_ID)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(accountResponse);

        AccountResponse result = accountService.getMyAccount(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        verify(accountRules).findByUserIdOrThrow(USER_ID);
        verify(accountMapper).toResponse(activeAccount);
    }

    @Test
    void getMyAccount_WhenAccountNotFound_ShouldThrowWalletXException() {
        when(accountRules.findByUserIdOrThrow(USER_ID))
                .thenThrow(new WalletXException(HttpStatus.NOT_FOUND, Constants.MessageKeys.ACCOUNT_NOT_FOUND));

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.getMyAccount(USER_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(accountMapper, never()).toResponse(any());
    }

    // ================================
    // updateMyStatus
    // ================================

    @Test
    void updateMyStatus_WhenValidTransition_ShouldReturnUpdatedAccountResponse() {
        AccountResponse frozenResponse = AccountResponse.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .userId(USER_ID)
                .status(AccountStatus.FROZEN)
                .build();

        when(accountRules.findByUserIdOrThrow(USER_ID)).thenReturn(activeAccount);
        when(accountRepository.save(activeAccount)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(frozenResponse);

        AccountResponse result = accountService.updateMyStatus(USER_ID, updateStatusRequest);

        assertNotNull(result);
        verify(accountRules).findByUserIdOrThrow(USER_ID);
        verify(accountRules).validateStatusTransition(activeAccount, AccountStatus.FROZEN);
        verify(accountRepository).save(activeAccount);
        verify(accountMapper).toResponse(activeAccount);
    }

    @Test
    void updateMyStatus_WhenAccountNotFound_ShouldThrowWalletXException() {
        when(accountRules.findByUserIdOrThrow(USER_ID))
                .thenThrow(new WalletXException(HttpStatus.NOT_FOUND, Constants.MessageKeys.ACCOUNT_NOT_FOUND));

        assertThrows(WalletXException.class, () -> accountService.updateMyStatus(USER_ID, updateStatusRequest));

        verify(accountRules, never()).validateStatusTransition(any(), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateMyStatus_WhenAccountIsClosed_ShouldThrowWalletXException() {
        when(accountRules.findByUserIdOrThrow(USER_ID)).thenReturn(closedAccount);
        doThrow(new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, Constants.MessageKeys.ACCOUNT_INVALID_STATUS_TRANSITION))
                .when(accountRules).validateStatusTransition(closedAccount, AccountStatus.FROZEN);

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.updateMyStatus(USER_ID, updateStatusRequest)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(accountRepository, never()).save(any());
    }

    // ================================
    // deposit
    // ================================

    @Test
    void deposit_WhenValidRequest_ShouldReturnUpdatedBalance() {
        BigDecimal depositAmount = BigDecimal.valueOf(500);
        BigDecimal expectedBalance = BigDecimal.valueOf(1500);

        AccountResponse depositResponse = AccountResponse.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .userId(USER_ID)
                .balance(expectedBalance)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRules.findByUserIdOrThrow(USER_ID)).thenReturn(activeAccount);
        when(accountRepository.save(activeAccount)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(depositResponse);

        AccountResponse result = accountService.deposit(USER_ID, depositRequest);

        assertNotNull(result);
        assertEquals(expectedBalance, result.getBalance());

        verify(accountRules).validateDepositAmount(depositAmount);
        verify(accountRules).findByUserIdOrThrow(USER_ID);
        verify(accountRules).validateAccountIsActive(activeAccount);
        verify(accountRepository).save(activeAccount);
    }

    @Test
    void deposit_WhenInvalidAmount_ShouldThrowWalletXException() {
        doThrow(new WalletXException(HttpStatus.BAD_REQUEST, Constants.MessageKeys.ACCOUNT_DEPOSIT_INVALID_AMOUNT))
                .when(accountRules).validateDepositAmount(depositRequest.getAmount());

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.deposit(USER_ID, depositRequest)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(accountRules, never()).findByUserIdOrThrow(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_WhenAccountIsFrozen_ShouldThrowWalletXException() {
        when(accountRules.findByUserIdOrThrow(USER_ID)).thenReturn(frozenAccount);
        doThrow(new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, Constants.MessageKeys.ACCOUNT_FROZEN))
                .when(accountRules).validateAccountIsActive(frozenAccount);

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.deposit(USER_ID, depositRequest)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void deposit_WhenAccountIsClosed_ShouldThrowWalletXException() {
        when(accountRules.findByUserIdOrThrow(USER_ID)).thenReturn(closedAccount);
        doThrow(new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, Constants.MessageKeys.ACCOUNT_CLOSED))
                .when(accountRules).validateAccountIsActive(closedAccount);

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.deposit(USER_ID, depositRequest)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(accountRepository, never()).save(any());
    }

    // ================================
    // getAllAccounts
    // ================================

    @Test
    void getAllAccounts_WhenAccountsExist_ShouldReturnList() {
        when(accountRepository.findAll()).thenReturn(List.of(activeAccount, frozenAccount));
        when(accountMapper.toResponse(activeAccount)).thenReturn(accountResponse);
        when(accountMapper.toResponse(frozenAccount)).thenReturn(AccountResponse.builder().status(AccountStatus.FROZEN).build());

        List<AccountResponse> result = accountService.getAllAccounts();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(accountRepository).findAll();
        verify(accountMapper, times(2)).toResponse(any(Account.class));
    }

    @Test
    void getAllAccounts_WhenNoAccounts_ShouldReturnEmptyList() {
        when(accountRepository.findAll()).thenReturn(List.of());

        List<AccountResponse> result = accountService.getAllAccounts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(accountRepository).findAll();
        verify(accountMapper, never()).toResponse(any());
    }

    // ================================
    // getAccountById
    // ================================

    @Test
    void getAccountById_WhenAccountExists_ShouldReturnAccountResponse() {
        when(accountRules.findByIdOrThrow(ACCOUNT_ID)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(accountResponse);

        AccountResponse result = accountService.getAccountById(ACCOUNT_ID);

        assertNotNull(result);
        assertEquals(ACCOUNT_ID, result.getId());
        verify(accountRules).findByIdOrThrow(ACCOUNT_ID);
        verify(accountMapper).toResponse(activeAccount);
    }

    @Test
    void getAccountById_WhenAccountNotFound_ShouldThrowWalletXException() {
        when(accountRules.findByIdOrThrow(ACCOUNT_ID))
                .thenThrow(new WalletXException(HttpStatus.NOT_FOUND, Constants.MessageKeys.ACCOUNT_NOT_FOUND));

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.getAccountById(ACCOUNT_ID)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(accountMapper, never()).toResponse(any());
    }

    // ================================
    // updateAccountStatus (admin)
    // ================================

    @Test
    void updateAccountStatus_WhenValidTransition_ShouldReturnUpdatedAccountResponse() {
        when(accountRules.findByIdOrThrow(ACCOUNT_ID)).thenReturn(activeAccount);
        when(accountRepository.save(activeAccount)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(accountResponse);

        AccountResponse result = accountService.updateAccountStatus(ACCOUNT_ID, updateStatusRequest);

        assertNotNull(result);
        verify(accountRules).findByIdOrThrow(ACCOUNT_ID);
        verify(accountRules).validateStatusTransition(activeAccount, AccountStatus.FROZEN);
        verify(accountRepository).save(activeAccount);
        verify(accountMapper).toResponse(activeAccount);
    }

    @Test
    void updateAccountStatus_WhenAccountNotFound_ShouldThrowWalletXException() {
        when(accountRules.findByIdOrThrow(ACCOUNT_ID))
                .thenThrow(new WalletXException(HttpStatus.NOT_FOUND, Constants.MessageKeys.ACCOUNT_NOT_FOUND));

        assertThrows(WalletXException.class, () -> accountService.updateAccountStatus(ACCOUNT_ID, updateStatusRequest));

        verify(accountRules, never()).validateStatusTransition(any(), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateAccountStatus_WhenAccountIsClosed_ShouldThrowWalletXException() {
        when(accountRules.findByIdOrThrow(ACCOUNT_ID)).thenReturn(closedAccount);
        doThrow(new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, Constants.MessageKeys.ACCOUNT_INVALID_STATUS_TRANSITION))
                .when(accountRules).validateStatusTransition(closedAccount, AccountStatus.FROZEN);

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.updateAccountStatus(ACCOUNT_ID, updateStatusRequest)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(accountRepository, never()).save(any());
    }

    // ================================
    // getAccountByAccountNumber
    // ================================

    @Test
    void getAccountByAccountNumber_WhenAccountExists_ShouldReturnAccountResponse() {
        when(accountRules.findByAccountNumberOrThrow(ACCOUNT_NUMBER)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(accountResponse);

        AccountResponse result = accountService.getAccountByAccountNumber(ACCOUNT_NUMBER);

        assertNotNull(result);
        assertEquals(ACCOUNT_NUMBER, result.getAccountNumber());
        verify(accountRules).findByAccountNumberOrThrow(ACCOUNT_NUMBER);
        verify(accountMapper).toResponse(activeAccount);
    }

    @Test
    void getAccountByAccountNumber_WhenAccountNotFound_ShouldThrowWalletXException() {
        when(accountRules.findByAccountNumberOrThrow(ACCOUNT_NUMBER))
                .thenThrow(new WalletXException(HttpStatus.NOT_FOUND, Constants.MessageKeys.ACCOUNT_NOT_FOUND));

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.getAccountByAccountNumber(ACCOUNT_NUMBER)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(accountMapper, never()).toResponse(any());
    }

    // ================================
    // existsByAccountNumber
    // ================================

    @Test
    void existsByAccountNumber_WhenAccountExists_ShouldReturnTrue() {
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(true);

        boolean result = accountService.existsByAccountNumber(ACCOUNT_NUMBER);

        assertTrue(result);
        verify(accountRepository).existsByAccountNumber(ACCOUNT_NUMBER);
    }

    @Test
    void existsByAccountNumber_WhenAccountDoesNotExist_ShouldReturnFalse() {
        when(accountRepository.existsByAccountNumber(ACCOUNT_NUMBER)).thenReturn(false);

        boolean result = accountService.existsByAccountNumber(ACCOUNT_NUMBER);

        assertFalse(result);
        verify(accountRepository).existsByAccountNumber(ACCOUNT_NUMBER);
    }

    // ================================
    // updateBalance
    // ================================

    @Test
    void updateBalance_WhenActiveAccount_ShouldAddAmountAndReturnResponse() {
        BigDecimal delta = BigDecimal.valueOf(200);
        BigDecimal expectedBalance = BigDecimal.valueOf(1200);

        AccountResponse balanceResponse = AccountResponse.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .balance(expectedBalance)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRules.findByAccountNumberOrThrow(ACCOUNT_NUMBER)).thenReturn(activeAccount);
        when(accountRepository.save(activeAccount)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(balanceResponse);

        AccountResponse result = accountService.updateBalance(ACCOUNT_NUMBER, delta);

        assertNotNull(result);
        assertEquals(expectedBalance, result.getBalance());

        verify(accountRules).findByAccountNumberOrThrow(ACCOUNT_NUMBER);
        verify(accountRules).validateAccountIsActive(activeAccount);
        verify(accountRepository).save(activeAccount);
        verify(accountMapper).toResponse(activeAccount);
    }

    @Test
    void updateBalance_WhenAccountNotFound_ShouldThrowWalletXException() {
        when(accountRules.findByAccountNumberOrThrow(ACCOUNT_NUMBER))
                .thenThrow(new WalletXException(HttpStatus.NOT_FOUND, Constants.MessageKeys.ACCOUNT_NOT_FOUND));

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.updateBalance(ACCOUNT_NUMBER, BigDecimal.valueOf(100))
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(accountRules, never()).validateAccountIsActive(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateBalance_WhenAccountIsFrozen_ShouldThrowWalletXException() {
        when(accountRules.findByAccountNumberOrThrow(ACCOUNT_NUMBER)).thenReturn(frozenAccount);
        doThrow(new WalletXException(HttpStatus.UNPROCESSABLE_ENTITY, Constants.MessageKeys.ACCOUNT_FROZEN))
                .when(accountRules).validateAccountIsActive(frozenAccount);

        WalletXException exception = assertThrows(
                WalletXException.class,
                () -> accountService.updateBalance(ACCOUNT_NUMBER, BigDecimal.valueOf(100))
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void updateBalance_WhenNegativeAmount_ShouldSubtractBalanceAndReturnResponse() {
        BigDecimal delta = BigDecimal.valueOf(-300);
        BigDecimal expectedBalance = BigDecimal.valueOf(700);

        AccountResponse balanceResponse = AccountResponse.builder()
                .id(ACCOUNT_ID)
                .accountNumber(ACCOUNT_NUMBER)
                .balance(expectedBalance)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRules.findByAccountNumberOrThrow(ACCOUNT_NUMBER)).thenReturn(activeAccount);
        when(accountRepository.save(activeAccount)).thenReturn(activeAccount);
        when(accountMapper.toResponse(activeAccount)).thenReturn(balanceResponse);

        AccountResponse result = accountService.updateBalance(ACCOUNT_NUMBER, delta);

        assertNotNull(result);
        assertEquals(expectedBalance, result.getBalance());
        verify(accountRules).validateAccountIsActive(activeAccount);
        verify(accountRepository).save(activeAccount);
    }

}
