package com.walletx.authservice.utils.constant;

public class Constants {
    private Constants() {}

    public static final class Security {
        private Security() {}

        public static final String BEARER_PREFIX = "Bearer ";
        public static final String AUTH_HEADER = "Authorization";
        public static final String TOKEN_TYPE = "Bearer";
    }

    public static final class Validation {
        private Validation() {}

        public static final int PASSWORD_MIN_LENGTH = 8;
        public static final int EMAIL_MAX_LENGTH = 100;
        public static final int NAME_MAX_LENGTH = 150;
        public static final int PHONE_MAX_LENGTH = 20;
    }

    public static final class MessageKeys {
        private MessageKeys() {}

        // Error keys - User
        public static final String USER_NOT_FOUND = "error.user.not.found";
        public static final String USER_ALREADY_EXISTS = "error.user.already.exists";
        public static final String INVALID_CREDENTIALS = "error.invalid.credentials";
        public static final String ROLE_NOT_FOUND = "error.role.not.found";
        public static final String ACCOUNT_DISABLED = "error.account.disabled";

        // Error keys - Token
        public static final String TOKEN_INVALID_SIGNATURE = "error.token.invalid.signature";
        public static final String TOKEN_MALFORMED = "error.token.malformed";
        public static final String TOKEN_EXPIRED = "error.token.expired";
        public static final String TOKEN_EMPTY = "error.token.empty";
        public static final String TOKEN_REVOKED = "error.token.revoked";

        // Error keys - General
        public static final String ACCESS_DENIED = "error.access.denied";
        public static final String UNEXPECTED_ERROR = "error.unexpected";

        // Success keys
        public static final String USER_REGISTERED = "success.user.registered";
        public static final String USER_LOGGED_OUT = "success.user.logged.out";
    }
}
