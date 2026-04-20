package com.walletx.accountservice.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositRequest {

    @NotNull(message = "{account.deposit.amount.not_null}")
    @Positive(message = "{account.deposit.amount.positive}")
    private BigDecimal amount;

}
