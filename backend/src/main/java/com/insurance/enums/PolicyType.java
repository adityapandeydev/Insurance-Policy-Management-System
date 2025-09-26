package com.insurance.enums;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                       POLICY TYPE ENUM                              ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  Defines the types of insurance policies supported by the system.   ║
 * ║                                                                      ║
 * ║  Each policy type has different:                                     ║
 * ║  • Base premium rates (stored in PolicyService business logic)      ║
 * ║  • Coverage characteristics                                          ║
 * ║  • Risk scoring weight adjustments                                  ║
 * ║                                                                      ║
 * ║  INTERVIEW TIP: Using an enum instead of a plain String field:      ║
 * ║  1. Compile-time safety — invalid policy types cause compile errors  ║
 * ║  2. Switch expressions work cleanly with enums                      ║
 * ║  3. Can add behavior via abstract methods per enum constant          ║
 * ║  4. Database validation — only defined values can be stored         ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public enum PolicyType {

    /**
     * Life Insurance: Covers the policyholder's life.
     * Pays a death benefit to beneficiaries upon the insured's death.
     * Base premium rate: 0.5% of coverage amount per annum.
     */
    LIFE,

    /**
     * Health Insurance: Covers medical expenses, hospitalization, and treatment.
     * Base premium rate: 1.0% of coverage amount per annum.
     */
    HEALTH,

    /**
     * Vehicle Insurance: Covers damage to or by the insured vehicle.
     * Includes: collision, theft, third-party liability.
     * Base premium rate: 2.0% of coverage amount per annum.
     */
    VEHICLE,

    /**
     * Property Insurance: Covers real estate and personal property.
     * Includes: fire, flood, theft, natural disasters.
     * Base premium rate: 0.8% of coverage amount per annum.
     */
    PROPERTY,

    /**
     * Travel Insurance: Covers trip cancellations, medical emergencies abroad,
     * lost luggage, and flight delays.
     * Base premium rate: 3.0% of coverage amount per annum.
     */
    TRAVEL
}
