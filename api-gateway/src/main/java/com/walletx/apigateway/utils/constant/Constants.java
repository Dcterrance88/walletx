package com.walletx.apigateway.utils.constant;

public class Constants {

    private Constants() {}

    public static class Security {

        private Security() {}

        public static final String BEARER_PREFIX = "Bearer ";
        public static final String X_USER_ID_HEADER = "X-User-Id";
        public static final int JWT_FILTER_ORDER = -1;
        public static final int GATEWAY_EXCEPTION_HANDLER_ORDER = -2;

    }

    public static class PublicPaths {

        private PublicPaths() {}

        public static final String AUTH_LOGIN = "/api/v1/auth/login";
        public static final String AUTH_REGISTER = "/api/v1/auth/register";
        public static final String AUTH_REFRESH_TOKEN = "/api/v1/auth/refresh-token";
    }

    public static class ErrorResponse {

        private ErrorResponse() {}

        public static final String FIELD_CODE = "code";
        public static final String FIELD_MESSAGE = "message";
        public static final String FIELD_USER_MESSAGE = "userMessage";
        public static final String FIELD_PATH = "path";
        public static final String FIELD_TIMESTAMP = "timestamp";
        public static final String UNEXPECTED_ERROR = "An unexpected error occurred";
    }

}
