package com.walletx.authservice.exception;

import com.walletx.authservice.utils.message.MessageService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Global exception handler for all REST controllers in the WalletX auth service.
 *
 * <p>Intercepts exceptions thrown during request processing and returns structured
 * {@link ApiErrorResponse} objects. Separates developer-facing messages (technical details)
 * from user-facing messages (internationalized via {@link MessageService}).</p>
 *
 * <p>Note: This handler operates at the Spring MVC layer — it does not intercept
 * exceptions thrown at the filter level. For filter-level authentication errors,
 * see {@link com.walletx.authservice.security.JwtAuthenticationEntryPoint}.</p>
 *
 * @see ApiErrorResponse
 * @see WalletXException
 * @see MessageService
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    /**
     * Handles business logic exceptions thrown by the WalletX platform.
     *
     * <p>These exceptions are intentional — they represent expected failure
     * scenarios such as user not found, invalid token, or account disabled.</p>
     *
     * @param exception the business exception containing the HTTP status and message key
     * @param request   the current web request
     * @return a structured error response with the defined HTTP status
     */
    @ExceptionHandler(WalletXException.class)
    public ResponseEntity<ApiErrorResponse> handleWalletXException(
            WalletXException exception, WebRequest request) {

        log.warn("Business exception: status={}, key={}", exception.getStatus(), exception.getMessageKey());

        return buildResponse(
                exception.getStatus(),
                exception.getMessageKey(),
                messageService.getMessage(exception.getMessageKey()),
                request
        );
    }

    /**
     * Handles authentication failures caused by invalid email or password.
     *
     * <p>Triggered by Spring Security when credentials do not match.
     * Returns a generic message to avoid exposing whether the email
     * or password was incorrect — a security best practice.</p>
     *
     * @param exception the bad credentials exception
     * @param request   the current web request
     * @return a 401 Unauthorized error response
     */
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

    /**
     * Handles cases where the user is not found during authentication.
     *
     * @param exception the username not found exception
     * @param request   the current web request
     * @return a 401 Unauthorized error response
     */
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

    /**
     * Handles authorization failures when an authenticated user attempts
     * to access a resource they do not have permission for.
     *
     * @param exception the access denied exception
     * @param request   the current web request
     * @return a 403 Forbidden error response
     */
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

    /**
     * Handles validation errors for request bodies annotated with {@code @Valid}.
     *
     * <p>Collects all field-level validation errors and joins them into a single
     * user-friendly message. The developer-facing message contains the raw
     * exception details for debugging.</p>
     *
     * @param exception the validation exception containing field errors
     * @param request   the current web request
     * @return a 400 Bad Request error response with field validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception, WebRequest request) {

        String userMessage = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error: path={}, fields={}", request.getDescription(false), userMessage);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                userMessage,
                request
        );
    }

    /**
     * Handles constraint violations on method-level parameters such as
     * {@code @RequestParam} and {@code @PathVariable}.
     *
     * <p>Collects all violated constraints and joins them into a single
     * user-friendly message.</p>
     *
     * @param exception the constraint violation exception
     * @param request   the current web request
     * @return a 400 Bad Request error response with constraint violation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, WebRequest request) {

        String userMessage = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        log.warn("Constraint violation: path={}, violations={}", request.getDescription(false), userMessage);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                userMessage,
                request
        );
    }

    /**
     * Handles any unexpected exception not covered by the other handlers.
     *
     * <p>Acts as a safety net — logs the technical details internally while
     * returning a generic message to the user to avoid exposing
     * sensitive system information.</p>
     *
     * @param exception the unexpected exception
     * @param request   the current web request
     * @return a 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(
            Exception exception, WebRequest request) {

        log.error("Unexpected error: path={}, message={}", request.getDescription(false), exception.getMessage());

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                messageService.getMessage(UNEXPECTED_ERROR),
                request
        );
    }

    /**
     * Builds a structured {@link ApiErrorResponse} and wraps it in a {@link ResponseEntity}.
     *
     * <p>Used internally by all exception handlers to ensure a consistent
     * response format across the entire service.</p>
     *
     * @param status      the HTTP status to return
     * @param code        the technical error code or message key
     * @param userMessage the internationalized user-facing error message
     * @param request     the current web request used to extract the request path
     * @return a {@link ResponseEntity} containing the structured error response
     */
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
