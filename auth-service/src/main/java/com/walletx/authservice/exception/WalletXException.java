package com.walletx.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class WalletXException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String messageKey;

    public WalletXException(HttpStatus status, String messageKey) {
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
    }

}
