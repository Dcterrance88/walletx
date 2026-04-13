package com.walletx.accountservice.domain.dto.request;

import com.walletx.accountservice.domain.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAccountStatusRequest {

    @NotNull(message = "{account.status.not_null}")
    private AccountStatus status;

}
