package com.insurance.dto.response;

import com.insurance.enums.ClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for claim data including policy and customer context.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {

    private Long id;
    private String claimNumber;
    private String description;
    private BigDecimal claimAmount;
    private ClaimStatus status;
    private String reviewNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDate incidentDate;

    // Policy context
    private Long policyId;
    private String policyNumber;
    private String policyName;
    private BigDecimal policyCoverageAmount;

    // Customer context
    private Long customerId;
    private String customerName;
    private String customerEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
