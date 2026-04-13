package com.walletx.accountservice.domain.dto.request;

import com.walletx.accountservice.domain.enums.Currency;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountRequest {

    @NotNull(message = "{account.create.currency.not_null}")
    private Currency currency;

}
