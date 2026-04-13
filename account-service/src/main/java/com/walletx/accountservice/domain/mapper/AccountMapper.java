package com.walletx.accountservice.domain.mapper;

import com.walletx.accountservice.domain.dto.response.AccountResponse;
import com.walletx.accountservice.domain.entity.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

}
