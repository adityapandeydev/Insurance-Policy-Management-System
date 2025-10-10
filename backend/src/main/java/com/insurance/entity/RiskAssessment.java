package com.insurance.entity;

import com.insurance.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                      RISK ASSESSMENT ENTITY                             ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Stores the computed risk profile for a customer.                       ║
 * ║  Recalculated whenever: customer profile changes, new claim is filed.   ║
 * ║                                                                          ║
 * ║  RELATIONSHIP: Customer (1) ─── (1) RiskAssessment                     ║
 * ║  RiskAssessment owns the FK (customer_id), making it the owning side.  ║
 * ║                                                                          ║
 * ║  SCORING ALGORITHM:                                                     ║
 * ║  totalRiskScore = (ageScore × 0.3) + (coverageScore × 0.4)             ║
 * ║                 + (claimHistoryScore × 0.3)                             ║
 * ║                                                                          ║
 * ║  Age scoring:                                                            ║
 * ║    <25  → 8 pts  (young drivers: high accident risk)                   ║
 * ║    25-40 → 3 pts (prime age: lowest risk bracket)                      ║
 * ║    40-60 → 6 pts (increasing health risk)                              ║
 * ║    >60  → 9 pts  (elderly: highest life/health risk)                  ║
 * ║                                                                          ║
 * ║  Coverage scoring:                                                       ║
 * ║    <100k      → 2 pts   (low exposure)                                 ║
 * ║    100k-500k  → 5 pts   (moderate exposure)                            ║
 * ║    500k-1M    → 8 pts   (high exposure)                                ║
 * ║    >1M        → 10 pts  (very high exposure)                           ║
 * ║                                                                          ║
 * ║  Claim history scoring:                                                  ║
 * ║    0 claims  → 1 pt    (clean history)                                 ║
 * ║    1-2       → 4 pts   (some history)                                  ║
 * ║    3-5       → 7 pts   (concerning pattern)                            ║
 * ║    >5        → 10 pts  (serial claimant)                               ║
 * ║                                                                          ║
 * ║  Risk Level:  <4 → LOW,  4-7 → MEDIUM,  >7 → HIGH                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(
    name = "risk_assessments",
    indexes = {
        @Index(name = "idx_risk_customer_id", columnList = "customer_id", unique = true),
        @Index(name = "idx_risk_level", columnList = "risk_level")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @OneToOne: One RiskAssessment per Customer.
     * @JoinColumn: This table owns the FK (customer_id).
     *   RiskAssessment is the OWNING side of the Customer-RiskAssessment relationship.
     * unique = true on @JoinColumn → enforces one-to-one at DB level.
     *
     * INTERVIEW TIP: For @OneToOne, you must decide which side holds the FK.
     * Best practice: put FK on the "child" or "dependent" side.
     * RiskAssessment depends on Customer → RiskAssessment holds the FK.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    // ─── INDIVIDUAL SCORE COMPONENTS ─────────────────────────────────────
    // Stored individually for: transparency, debugging, re-weighting without recalculation

    /**
     * Age-based risk score (0-10).
     * Uses Integer (not int primitive) to allow NULL for unassessed records.
     */
    @Column(name = "age_score", nullable = false)
    @Builder.Default
    private Integer ageScore = 0;

    /**
     * Coverage-based risk score (0-10).
     * Higher coverage = higher insurer's financial exposure = higher risk score.
     */
    @Column(name = "coverage_score", nullable = false)
    @Builder.Default
    private Integer coverageScore = 0;

    /**
     * Historical claim count risk score (0-10).
     * More past claims = higher probability of future claims = higher risk.
     */
    @Column(name = "claim_history_score", nullable = false)
    @Builder.Default
    private Integer claimHistoryScore = 0;

    // ─── TOTAL WEIGHTED SCORE ─────────────────────────────────────────────

    /**
     * Weighted total: (ageScore × 0.3) + (coverageScore × 0.4) + (claimHistoryScore × 0.3)
     * Range: 0.00 to 10.00
     * NUMERIC(5,2) → e.g., 7.50, 3.20
     */
    @Column(name = "total_risk_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal totalRiskScore = BigDecimal.ZERO;

    /**
     * Categorical risk level derived from totalRiskScore:
     * LOW (<4.0), MEDIUM (4.0-7.0), HIGH (>7.0)
     * Used by PolicyService to determine premium multiplier.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    @Builder.Default
    private RiskLevel riskLevel = RiskLevel.LOW;

    /**
     * Human-readable explanation of the risk assessment.
     * Shown to the customer or agent explaining the risk level.
     */
    @Column(name = "assessment_notes", columnDefinition = "TEXT")
    private String assessmentNotes;

    /**
     * When was this assessment last calculated?
     * Allows tracking when the risk profile was last updated.
     */
    @Column(name = "assessed_at", nullable = false)
    @Builder.Default
    private LocalDateTime assessedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
