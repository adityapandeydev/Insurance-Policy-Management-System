package com.insurance.enums;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                      POLICY STATUS ENUM                             ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  Represents the lifecycle states of an insurance policy.            ║
 * ║                                                                      ║
 * ║  Policy State Machine:                                              ║
 * ║                                                                      ║
 * ║           ┌─────────┐                                               ║
 * ║           │ PENDING │ ←── Initial state on creation                 ║
 * ║           └────┬────┘                                               ║
 * ║                │  Agent/Admin approves                              ║
 * ║           ┌────▼────┐                                               ║
 * ║           │ ACTIVE  │ ←── Policy is in force, claims can be made   ║
 * ║           └────┬────┘                                               ║
 * ║           ┌────┴──────────┐                                         ║
 * ║           │               │                                         ║
 * ║    endDate passed    Customer/Agent cancels                         ║
 * ║           │               │                                         ║
 * ║      ┌────▼────┐    ┌─────▼─────┐                                  ║
 * ║      │ EXPIRED │    │ CANCELLED │                                   ║
 * ║      └─────────┘    └───────────┘                                  ║
 * ║                                                                      ║
 * ║  INTERVIEW TIP: Why store status as a String (EnumType.STRING)?     ║
 * ║  @Enumerated(EnumType.ORDINAL) stores the enum index (0, 1, 2).    ║
 * ║  Problem: If you add/reorder enum values, old data becomes corrupt. ║
 * ║  @Enumerated(EnumType.STRING) stores "ACTIVE", "EXPIRED" etc.      ║
 * ║  → Human-readable, rename-safe, order-independent. Always use this. ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public enum PolicyStatus {

    /**
     * Policy has been created but not yet activated by an agent or admin.
     * Claims CANNOT be submitted for PENDING policies.
     */
    PENDING,

    /**
     * Policy is active and in force. Claims CAN be submitted.
     * This state requires: current date is between startDate and endDate.
     */
    ACTIVE,

    /**
     * Policy has passed its endDate and is no longer in force.
     * Claims CANNOT be submitted for EXPIRED policies.
     * INTERVIEW TIP: Expiry can be set automatically via a @Scheduled job.
     */
    EXPIRED,

    /**
     * Policy was manually cancelled by the customer or an agent/admin.
     * Claims CANNOT be submitted for CANCELLED policies.
     */
    CANCELLED
}
