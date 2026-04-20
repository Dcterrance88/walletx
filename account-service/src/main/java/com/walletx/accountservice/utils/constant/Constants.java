package com.walletx.accountservice.utils.constant;

public class Constants {

    private Constants() {}

    public static class MessageKeys {

        private MessageKeys() {}

        // Account
        public static final String ACCOUNT_NOT_FOUND = "account.not_found";
        public static final String ACCOUNT_ALREADY_EXISTS = "account.already_exists";
        public static final String ACCOUNT_FROZEN = "account.frozen";
        public static final String ACCOUNT_CLOSED = "account.closed";
        public static final String ACCOUNT_INSUFFICIENT_FUNDS = "account.insufficient_funds";
        public static final String ACCOUNT_INVALID_STATUS_TRANSITION = "account.invalid_status_transition";
        public static final String ACCOUNT_DEPOSIT_INVALID_AMOUNT = "account.deposit.invalid_amount";
    }

}
