package com.insurance.dto.response;

import com.insurance.enums.PolicyStatus;
import com.insurance.enums.PolicyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for policy data.
 * Includes denormalized customer info (name, email) to avoid nested DTO fetching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private Long id;
    private String policyNumber;
    private String policyName;
    private PolicyType policyType;
    private String description;
    private BigDecimal coverageAmount;
    private BigDecimal premiumAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private PolicyStatus status;
    private String premiumFrequency;

    // Denormalized customer info
    private Long customerId;
    private String customerName;
    private String customerEmail;

    // Claim summary
    private long totalClaims;
    private long approvedClaims;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
