package com.insurance.service;

import com.insurance.dto.response.RiskAssessmentResponse;
import com.insurance.entity.Customer;
import com.insurance.entity.RiskAssessment;
import com.insurance.enums.RiskLevel;
import com.insurance.exception.ResourceNotFoundException;
import com.insurance.repository.ClaimRepository;
import com.insurance.repository.CustomerRepository;
import com.insurance.repository.PolicyRepository;
import com.insurance.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                     RISK ASSESSMENT SERVICE                             ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  SCORING ALGORITHM:                                                     ║
 * ║                                                                          ║
 * ║  1. AGE SCORE (0-10, weight: 30%):                                     ║
 * ║     Age < 25:    8 pts  (young = inexperienced, higher accident risk)  ║
 * ║     Age 25-40:   3 pts  (prime working age, lowest risk bracket)       ║
 * ║     Age 41-60:   6 pts  (increasing health concerns, some risk)        ║
 * ║     Age > 60:    9 pts  (elderly = high health/life insurance risk)    ║
 * ║                                                                          ║
 * ║  2. COVERAGE SCORE (0-10, weight: 40%):                                ║
 * ║     Total coverage < 100,000:      2 pts  (low financial exposure)     ║
 * ║     Total coverage 100k-500k:      5 pts  (moderate exposure)          ║
 * ║     Total coverage 500k-1M:        8 pts  (high exposure)              ║
 * ║     Total coverage > 1M:          10 pts  (very high financial risk)   ║
 * ║                                                                          ║
 * ║  3. CLAIM HISTORY SCORE (0-10, weight: 30%):                           ║
 * ║     0 approved claims:  1 pt   (clean record, preferred customer)      ║
 * ║     1-2 claims:         4 pts  (some history, within normal range)     ║
 * ║     3-5 claims:         7 pts  (frequent claimant, elevated risk)      ║
 * ║     > 5 claims:        10 pts  (serial claimant, very high risk)       ║
 * ║                                                                          ║
 * ║  TOTAL SCORE = (ageScore × 0.3) + (coverageScore × 0.4) + (histScore × 0.3) ║
 * ║  LOW: < 4.0  |  MEDIUM: 4.0-7.0  |  HIGH: > 7.0                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RiskAssessmentService {

    private final RiskAssessmentRepository riskAssessmentRepository;
    private final CustomerRepository customerRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;

    // Score weights (must sum to 1.0)
    private static final BigDecimal AGE_WEIGHT = new BigDecimal("0.3");
    private static final BigDecimal COVERAGE_WEIGHT = new BigDecimal("0.4");
    private static final BigDecimal HISTORY_WEIGHT = new BigDecimal("0.3");

    /**
     * Calculates or recalculates the risk assessment for a customer.
     * Called: on demand, or triggered after a claim is approved/rejected.
     *
     * @param customerId Customer ID to assess
     * @return RiskAssessmentResponse with scores and risk level
     */
    public RiskAssessmentResponse assessCustomerRisk(Long customerId) {
        log.info("Calculating risk assessment for customer: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // ─── Calculate individual component scores ────────────────────────
        int ageScore = calculateAgeScore(customer.getAge());
        int coverageScore = calculateCoverageScore(customerId);
        int claimHistoryScore = calculateClaimHistoryScore(customerId);

        // ─── Calculate weighted total score ──────────────────────────────
        BigDecimal total = BigDecimal.valueOf(ageScore).multiply(AGE_WEIGHT)
                .add(BigDecimal.valueOf(coverageScore).multiply(COVERAGE_WEIGHT))
                .add(BigDecimal.valueOf(claimHistoryScore).multiply(HISTORY_WEIGHT))
                .setScale(2, RoundingMode.HALF_UP);

        // ─── Determine risk level ─────────────────────────────────────────
        RiskLevel riskLevel = determineRiskLevel(total);

        // ─── Build assessment notes ───────────────────────────────────────
        String notes = buildAssessmentNotes(ageScore, coverageScore, claimHistoryScore, riskLevel, customer);

        // ─── Save or update the risk assessment ──────────────────────────
        RiskAssessment assessment = riskAssessmentRepository.findByCustomerId(customerId)
                .orElse(RiskAssessment.builder().customer(customer).build());

        assessment.setAgeScore(ageScore);
        assessment.setCoverageScore(coverageScore);
        assessment.setClaimHistoryScore(claimHistoryScore);
        assessment.setTotalRiskScore(total);
        assessment.setRiskLevel(riskLevel);
        assessment.setAssessmentNotes(notes);
        assessment.setAssessedAt(LocalDateTime.now());

        RiskAssessment savedAssessment = riskAssessmentRepository.save(assessment);
        log.info("Risk assessment for customer {}: {} (score: {})", customerId, riskLevel, total);

        return buildResponse(savedAssessment, customer);
    }

    /**
     * Retrieves an existing risk assessment for a customer.
     *
     * @param customerId Customer ID
     * @return RiskAssessmentResponse or triggers assessment if none exists
     */
    @Transactional(readOnly = true)
    public RiskAssessmentResponse getRiskAssessment(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        RiskAssessment assessment = riskAssessmentRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Risk Assessment", "customerId", customerId
                ));

        return buildResponse(assessment, customer);
    }

    // ════════════════════════════════════════════════════════════════════════
    // SCORING ALGORITHM IMPLEMENTATIONS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Calculates age-based risk score.
     * Younger and older customers present higher risks (U-curve effect).
     */
    private int calculateAgeScore(int age) {
        if (age < 25) return 8;        // young: inexperienced, higher accident probability
        if (age <= 40) return 3;       // prime age: statistically lowest risk
        if (age <= 60) return 6;       // middle-age: increasing health risks
        return 9;                      // elderly: high life/health insurance risk
    }

    /**
     * Calculates coverage-based risk score from total active policy coverage.
     * Higher total coverage = higher financial exposure for the insurer.
     */
    private int calculateCoverageScore(Long customerId) {
        // Sum coverage amounts of all ACTIVE policies for this customer
        BigDecimal totalCoverage = policyRepository.findByCustomerId(customerId).stream()
                .filter(p -> p.isActiveAndInForce())
                .map(p -> p.getCoverageAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCoverage.compareTo(new BigDecimal("100000")) < 0) return 2;   // < 100k
        if (totalCoverage.compareTo(new BigDecimal("500000")) < 0) return 5;   // 100k-500k
        if (totalCoverage.compareTo(new BigDecimal("1000000")) < 0) return 8;  // 500k-1M
        return 10;                                                               // > 1M
    }

    /**
     * Calculates claim history risk score based on number of approved claims.
     * More approved claims = established pattern = higher future claim probability.
     */
    private int calculateClaimHistoryScore(Long customerId) {
        long approvedClaims = claimRepository.countApprovedClaimsByCustomer(customerId);

        if (approvedClaims == 0) return 1;          // clean history
        if (approvedClaims <= 2) return 4;          // some history
        if (approvedClaims <= 5) return 7;          // concerning pattern
        return 10;                                   // serial claimant
    }

    /**
     * Determines the risk category based on the total score.
     * Thresholds: LOW < 4.0 | 4.0 ≤ MEDIUM ≤ 7.0 | HIGH > 7.0
     */
    private RiskLevel determineRiskLevel(BigDecimal totalScore) {
        if (totalScore.compareTo(new BigDecimal("4.0")) < 0) return RiskLevel.LOW;
        if (totalScore.compareTo(new BigDecimal("7.0")) <= 0) return RiskLevel.MEDIUM;
        return RiskLevel.HIGH;
    }

    /**
     * Builds a human-readable assessment explanation.
     */
    private String buildAssessmentNotes(int ageScore, int coverageScore,
                                         int claimHistoryScore, RiskLevel riskLevel,
                                         Customer customer) {
        String ageNote = ageScore <= 3 ? "favorable age profile"
                : ageScore <= 6 ? "moderate age-related risk"
                : "elevated age-related risk";

        String coverageNote = coverageScore <= 2 ? "low coverage exposure"
                : coverageScore <= 5 ? "moderate coverage exposure"
                : "high coverage exposure";

        String historyNote = claimHistoryScore <= 1 ? "clean claim history"
                : claimHistoryScore <= 4 ? "manageable claim history"
                : "elevated claim history";

        return String.format(
                "%s risk assessment: %s, %s, and %s.",
                riskLevel, ageNote, coverageNote, historyNote
        );
    }

    /**
     * Builds the RiskAssessmentResponse DTO from the entity.
     */
    private RiskAssessmentResponse buildResponse(RiskAssessment assessment, Customer customer) {
        double premiumMultiplier = switch (assessment.getRiskLevel()) {
            case LOW -> 1.0;
            case MEDIUM -> 1.5;
            case HIGH -> 2.0;
        };

        return RiskAssessmentResponse.builder()
                .id(assessment.getId())
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .ageScore(assessment.getAgeScore())
                .coverageScore(assessment.getCoverageScore())
                .claimHistoryScore(assessment.getClaimHistoryScore())
                .totalRiskScore(assessment.getTotalRiskScore())
                .riskLevel(assessment.getRiskLevel())
                .assessmentNotes(assessment.getAssessmentNotes())
                .premiumMultiplier(premiumMultiplier)
                .assessedAt(assessment.getAssessedAt())
                .updatedAt(assessment.getUpdatedAt())
                .build();
    }
}
