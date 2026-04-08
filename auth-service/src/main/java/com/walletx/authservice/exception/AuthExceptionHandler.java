package com.walletx.authservice.exception;

import com.walletx.common.exception.ApiErrorResponse;
import com.walletx.common.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

import static com.walletx.authservice.utils.constant.Constants.MessageKeys.INVALID_CREDENTIALS;
import static com.walletx.authservice.utils.constant.Constants.MessageKeys.USER_NOT_FOUND;
import static com.walletx.common.exception.MessageKeys.ACCESS_DENIED;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException exception, WebRequest request) {

        log.warn("Bad credentials attempt: path={}", request.getDescription(false));

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                INVALID_CREDENTIALS,
                messageService.getMessage(INVALID_CREDENTIALS),
                request
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException exception, WebRequest request) {

        log.warn("User not found during authentication: {}", exception.getMessage());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                USER_NOT_FOUND,
                messageService.getMessage(USER_NOT_FOUND),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception, WebRequest request) {

        log.warn("Access denied: path={}", request.getDescription(false));

        return buildResponse(
                HttpStatus.FORBIDDEN,
                ACCESS_DENIED,
                messageService.getMessage(ACCESS_DENIED),
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String code, String userMessage, WebRequest request) {

        ApiErrorResponse response = ApiErrorResponse.builder()
                .code(status.toString())
                .message(code)
                .userMessage(userMessage)
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, status);
    }

}
