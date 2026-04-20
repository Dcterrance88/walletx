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
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.Locale;

import static com.walletx.authservice.utils.constant.Constants.MessageKeys.INVALID_CREDENTIALS;
import static com.walletx.authservice.utils.constant.Constants.MessageKeys.USER_NOT_FOUND;
import static com.walletx.common.exception.MessageKeys.ACCESS_DENIED;

/**
 * Security-specific exception handler for the WalletX auth service.
 *
 * <p>Handles Spring Security exceptions that cannot be intercepted by
 * {@link com.walletx.common.exception.GlobalExceptionHandler} because they
 * are thrown at the authentication layer, before reaching the MVC layer.</p>
 *
 * <p>All error messages are resolved according to the {@code Accept-Language}
 * header of the incoming request.</p>
 *
 * @see com.walletx.common.exception.GlobalExceptionHandler
 * @see MessageService
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class AuthExceptionHandler {

    private final MessageService messageService;

    /**
     * Handles failed authentication attempts due to invalid credentials.
     *
     * <p>Triggered when the provided email or password does not match
     * any registered user in the system.</p>
     *
     * @param exception the exception thrown by Spring Security on authentication failure
     * @param request   the current web request
     * @return a 401 Unauthorized error response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException exception, WebRequest request) {

        log.warn("Bad credentials attempt: path={}", request.getDescription(false));

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                messageService.getMessage(INVALID_CREDENTIALS, resolveLocale(request)),
                request
        );
    }

    /**
     * Handles cases where the authenticated user cannot be found in the system.
     *
     * <p>Triggered during token validation when the username extracted from
     * the JWT does not correspond to any existing user.</p>
     *
     * @param exception the exception thrown when the user is not found
     * @param request   the current web request
     * @return a 401 Unauthorized error response
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException exception, WebRequest request) {

        log.warn("User not found during authentication: {}", exception.getMessage());

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                messageService.getMessage(USER_NOT_FOUND, resolveLocale(request)),
                request
        );
    }

    /**
     * Handles attempts to access resources the authenticated user is not authorized for.
     *
     * <p>Triggered when a user with insufficient privileges attempts to access
     * a protected endpoint — for example, a regular user accessing an admin endpoint.</p>
     *
     * @param exception the exception thrown by Spring Security on authorization failure
     * @param request   the current web request
     * @return a 403 Forbidden error response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception, WebRequest request) {

        log.warn("Access denied: path={}", request.getDescription(false));

        return buildResponse(
                HttpStatus.FORBIDDEN,
                messageService.getMessage(ACCESS_DENIED, resolveLocale(request)),
                request
        );
    }

    /**
     * Resolves the locale from the {@code Accept-Language} header of the request.
     *
     * <p>Falls back to {@link Locale#ENGLISH} if the request is not a
     * {@link ServletWebRequest} or the header is absent.</p>
     *
     * @param request the current web request
     * @return the resolved {@link Locale}
     */
    private Locale resolveLocale(WebRequest request) {
        return request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getLocale()
                : Locale.ENGLISH;
    }

    /**
     * Builds a structured {@link ApiErrorResponse} and wraps it in a {@link ResponseEntity}.
     *
     * @param status      the HTTP status to return
     * @param userMessage the internationalized user-facing error message
     * @param request     the current web request used to extract the request path
     * @return a {@link ResponseEntity} containing the structured error response
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String userMessage, WebRequest request) {

        ApiErrorResponse response = ApiErrorResponse.builder()
                .code(status.toString())
                .userMessage(userMessage)
                .path(request.getDescription(false))
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, status);
    }

}
