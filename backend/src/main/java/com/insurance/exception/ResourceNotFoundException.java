package com.insurance.exception;

/**
 * Thrown when a requested resource is not found in the database.
 * Maps to HTTP 404 Not Found.
 *
 * INTERVIEW TIP: Custom exceptions hierarchy
 * ─────────────────────────────────────────────
 * RuntimeException (unchecked) → No forced try-catch at call sites.
 * Checked exceptions (extends Exception) → Caller MUST handle with try-catch.
 * In Spring applications, unchecked exceptions are preferred because:
 * 1. @Transactional only rolls back on RuntimeException by default
 * 2. Less boilerplate — service methods don't need throws declarations
 * 3. GlobalExceptionHandler catches them at the controller layer
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Creates a ResourceNotFoundException with a descriptive message.
     * Message format: "Customer not found with id: 42"
     *
     * @param resourceName The entity type (e.g., "Customer", "Policy")
     * @param fieldName    The field used for lookup (e.g., "id", "email")
     * @param fieldValue   The value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName() { return fieldName; }
    public Object getFieldValue() { return fieldValue; }
}
