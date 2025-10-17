package com.insurance.exception;

import com.insurance.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    GLOBAL EXCEPTION HANDLER                             ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Centralized error handling for the entire application.                 ║
 * ║  Intercepts exceptions thrown anywhere in the controller layer          ║
 * ║  and maps them to consistent, structured JSON error responses.          ║
 * ║                                                                          ║
 * ║  @RestControllerAdvice                                                  ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  = @ControllerAdvice + @ResponseBody                                    ║
 * ║  @ControllerAdvice: Applies advice to ALL @RestController classes       ║
 * ║  @ResponseBody: Returns objects serialized as JSON (not a view name)   ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: Without this class, Spring Boot returns its default     ║
 * ║  "whitelabel error page" (HTML) or a basic JSON error object.          ║
 * ║  This class ensures CONSISTENT error format across the entire API:      ║
 * ║  {                                                                       ║
 * ║    "success": false,                                                     ║
 * ║    "message": "Customer not found with id: 42",                        ║
 * ║    "data": null,                                                         ║
 * ║    "timestamp": "2024-05-15T10:30:00",                                 ║
 * ║    "statusCode": 404                                                    ║
 * ║  }                                                                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ════════════════════════════════════════════════════════════════════════
    // CUSTOM APPLICATION EXCEPTIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Handles ResourceNotFoundException → HTTP 404 Not Found.
     * Thrown when: customer, policy, claim, or risk assessment is not found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request
    ) {
        log.error("Resource not found: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles BusinessRuleException → HTTP 400 Bad Request.
     * Thrown when: claim on expired policy, amount exceeds coverage, etc.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessRuleException(
            BusinessRuleException ex,
            WebRequest request
    ) {
        log.error("Business rule violation: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles UnauthorizedException → HTTP 403 Forbidden.
     * Thrown when: customer tries to access another customer's data.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex,
            WebRequest request
    ) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), HttpStatus.FORBIDDEN.value());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ════════════════════════════════════════════════════════════════════════
    // VALIDATION EXCEPTIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Handles @Valid validation failures → HTTP 400 Bad Request.
     *
     * Thrown when: request body fails Bean Validation (@NotBlank, @Size, @Email, etc.)
     *
     * Returns a map of field → error message:
     * {
     *   "email": "must be a valid email address",
     *   "firstName": "must not be blank"
     * }
     *
     * INTERVIEW TIP: MethodArgumentNotValidException is thrown by Spring MVC
     * when @Valid fails. Each validation error is a FieldError with:
     * • field: the field that failed (e.g., "email")
     * • defaultMessage: the constraint message (e.g., "must not be blank")
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();

        // Collect all field validation errors into the map
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation failed: {}", errors);

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed. Please check the provided data.")
                .data(errors)
                .timestamp(LocalDateTime.now())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ════════════════════════════════════════════════════════════════════════
    // SPRING SECURITY EXCEPTIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Handles authentication failures → HTTP 401 Unauthorized.
     * Thrown when: login credentials are wrong (BadCredentialsException).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex
    ) {
        log.warn("Authentication failed: invalid credentials");
        ApiResponse<Object> response = ApiResponse.error(
                "Invalid email or password. Please try again.",
                HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles access denied (role mismatch) → HTTP 403 Forbidden.
     * Thrown when: @PreAuthorize check fails (user lacks required role).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex
    ) {
        log.warn("Access denied: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "You do not have permission to perform this action.",
                HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handles Spring Security authentication exceptions → HTTP 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex
    ) {
        log.warn("Authentication error: {}", ex.getMessage());
        ApiResponse<Object> response = ApiResponse.error(
                "Authentication failed: " + ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // ════════════════════════════════════════════════════════════════════════
    // GENERIC FALLBACK
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Catch-all handler for any unhandled exception → HTTP 500 Internal Server Error.
     * Logs the full stack trace but returns a generic message to the client
     * (we don't want to expose internal stack traces to API consumers).
     *
     * SECURITY TIP: Never expose exception stack traces in API responses.
     * Use a logging framework to record the error internally.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ApiResponse<Object> response = ApiResponse.error(
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
