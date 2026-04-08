package com.walletx.authservice.utils.constant;

public class Constants {
    private Constants() {}

    public static final class Security {
        private Security() {}

        public static final String BEARER_PREFIX = "Bearer ";
        public static final String AUTH_HEADER = "Authorization";
        public static final String TOKEN_TYPE = "Bearer";
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
        public static final String INVALID_TOKEN = "error.token.invalid";
    }
}
