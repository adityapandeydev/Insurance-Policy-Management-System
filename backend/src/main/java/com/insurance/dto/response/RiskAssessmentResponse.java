package com.insurance.dto.response;

import com.insurance.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for risk assessment data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentResponse {

    private Long id;
    private Long customerId;
    private String customerName;

    // Score breakdown
    private Integer ageScore;
    private Integer coverageScore;
    private Integer claimHistoryScore;
    private BigDecimal totalRiskScore;
    private RiskLevel riskLevel;

    // Readable explanation
    private String assessmentNotes;

    // Premium multiplier based on risk level
    private Double premiumMultiplier;

    private LocalDateTime assessedAt;
    private LocalDateTime updatedAt;
}
