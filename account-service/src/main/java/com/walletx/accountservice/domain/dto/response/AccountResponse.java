package com.walletx.accountservice.domain.dto.response;

import com.walletx.accountservice.domain.enums.AccountStatus;
import com.walletx.accountservice.domain.enums.AccountType;
import com.walletx.accountservice.domain.enums.Currency;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private Long userId;
    private BigDecimal balance;
    private Currency currency;
    private AccountType accountType;
    private AccountStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
