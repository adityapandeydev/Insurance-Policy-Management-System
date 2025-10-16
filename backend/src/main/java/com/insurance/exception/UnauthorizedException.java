package com.insurance.exception;

/**
 * Thrown when a user attempts to access a resource they do not own or have permission to view.
 * Maps to HTTP 403 Forbidden.
 *
 * Example: A CUSTOMER attempting to view another customer's policies.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
