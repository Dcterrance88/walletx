package com.walletx.authservice.exception;

import com.walletx.authservice.utils.message.MessageService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static com.walletx.authservice.utils.constant.Constants.MessageKeys.*;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(WalletXException.class)
    public ResponseEntity<ApiErrorResponse> handleWalletXException(
            WalletXException exception, WebRequest request) {

        return buildResponse(
                exception.getStatus(),
                exception.getMessageKey(),
                messageService.getMessage(exception.getMessageKey()),
                request
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException exception, WebRequest request) {

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

        return buildResponse(
                HttpStatus.FORBIDDEN,
                ACCESS_DENIED,
                messageService.getMessage(ACCESS_DENIED),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, WebRequest request) {

        String userMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                userMessage,
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, WebRequest request) {

        String userMessage = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                userMessage,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception exception, WebRequest request) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                messageService.getMessage(UNEXPECTED_ERROR),
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
