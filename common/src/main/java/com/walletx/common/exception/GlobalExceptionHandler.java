package com.walletx.common.exception;

import com.walletx.common.message.MessageService;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import static com.walletx.common.exception.MessageKeys.UNEXPECTED_ERROR;

/**
 * Global exception handler for all REST controllers in WalletX microservices.
 *
 * <p>Handles generic exceptions applicable to any microservice. Security-specific
 * exceptions (e.g. {@code BadCredentialsException}) are handled per-service
 * in their own {@code @RestControllerAdvice}.</p>
 *
 * <p>Note: This handler operates at the Spring MVC layer — it does not intercept
 * exceptions thrown at the filter level.</p>
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
     * Handles validation errors for request bodies annotated with {@code @Valid}.
     *
     * <p>Collects all field-level validation errors and joins them into a single
     * user-friendly message.</p>
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
     * returning a generic message to avoid exposing sensitive system information.</p>
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
