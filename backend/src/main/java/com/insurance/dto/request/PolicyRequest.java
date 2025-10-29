package com.insurance.dto.request;

import com.insurance.enums.PolicyType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating a policy.
 * Premium amount is NOT included — it's auto-calculated by the system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRequest {

    @NotBlank(message = "Policy name is required")
    @Size(max = 200, message = "Policy name must not exceed 200 characters")
    private String policyName;

    @NotNull(message = "Policy type is required")
    private PolicyType policyType;

    private String description;

    @NotNull(message = "Coverage amount is required")
    @DecimalMin(value = "1000.00", message = "Coverage amount must be at least 1000")
    @DecimalMax(value = "100000000.00", message = "Coverage amount cannot exceed 100,000,000")
    private BigDecimal coverageAmount;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be a positive number")
    private Long customerId;

    @NotBlank(message = "Premium frequency is required")
    @Pattern(regexp = "MONTHLY|QUARTERLY|SEMI_ANNUAL|ANNUAL",
             message = "Premium frequency must be one of: MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL")
    private String premiumFrequency;
}
