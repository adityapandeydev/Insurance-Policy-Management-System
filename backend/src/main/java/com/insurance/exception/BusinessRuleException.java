package com.insurance.exception;

/**
 * Thrown when a business rule is violated.
 * Maps to HTTP 400 Bad Request or 409 Conflict.
 *
 * Examples:
 * • Claiming against an expired policy
 * • Claim amount exceeds policy coverage
 * • Submitting duplicate policy number
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
