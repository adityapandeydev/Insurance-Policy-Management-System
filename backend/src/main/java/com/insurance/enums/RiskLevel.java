package com.insurance.enums;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                        RISK LEVEL ENUM                              ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  Categorizes a customer's overall risk profile.                     ║
 * ║                                                                      ║
 * ║  Risk Scoring Algorithm (see RiskAssessmentService for full impl):  ║
 * ║                                                                      ║
 * ║  totalScore = (ageScore × 0.3) + (coverageScore × 0.4)             ║
 * ║             + (claimHistoryScore × 0.3)                             ║
 * ║                                                                      ║
 * ║  Thresholds:                                                         ║
 * ║  totalScore < 4.0  → LOW    (preferred customers, lower premiums)  ║
 * ║  totalScore 4-7    → MEDIUM (standard customers, standard premiums) ║
 * ║  totalScore > 7.0  → HIGH   (high-risk customers, higher premiums) ║
 * ║                                                                      ║
 * ║  Risk affects premium calculation via riskMultiplier:              ║
 * ║  LOW    → 1.0x (no surcharge)                                      ║
 * ║  MEDIUM → 1.5x (50% surcharge)                                     ║
 * ║  HIGH   → 2.0x (100% surcharge)                                    ║
 * ║                                                                      ║
 * ║  INTERVIEW TIP: This enum is used in two contexts:                  ║
 * ║  1. RiskAssessment entity — stores assessed risk level              ║
 * ║  2. PolicyService — reads risk level to calculate premiums          ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public enum RiskLevel {

    /**
     * Low risk: totalScore < 4.0
     * Profile: young/middle-aged, low coverage, no or few prior claims.
     * Premium multiplier: 1.0x (base premium, no surcharge).
     */
    LOW,

    /**
     * Medium risk: totalScore between 4.0 and 7.0
     * Profile: older customers, moderate coverage, some claim history.
     * Premium multiplier: 1.5x (50% surcharge applied to base premium).
     */
    MEDIUM,

    /**
     * High risk: totalScore > 7.0
     * Profile: elderly customers, high coverage amounts, frequent claimers.
     * Premium multiplier: 2.0x (100% surcharge, double the base premium).
     */
    HIGH
}
