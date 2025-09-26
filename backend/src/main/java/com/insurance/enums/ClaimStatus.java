package com.insurance.enums;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                       CLAIM STATUS ENUM                             ║
 * ╠══════════════════════════════════════════════════════════════════════╣
 * ║  Represents the lifecycle states of an insurance claim.             ║
 * ║                                                                      ║
 * ║  Claim Workflow:                                                     ║
 * ║                                                                      ║
 * ║  Customer submits claim                                             ║
 * ║           │                                                          ║
 * ║      ┌────▼──────┐                                                  ║
 * ║      │  PENDING  │ ←── Newly submitted, awaiting review             ║
 * ║      └────┬──────┘                                                  ║
 * ║           │  Agent/Admin reviews                                    ║
 * ║      ┌────▼──────┐                                                  ║
 * ║      │ UNDER_    │ ←── Claim is being actively reviewed             ║
 * ║      │ REVIEW    │                                                  ║
 * ║      └────┬──────┘                                                  ║
 * ║      ┌────┴────────────────┐                                        ║
 * ║      │                     │                                        ║
 * ║  Approved             Rejected                                      ║
 * ║      │                     │                                        ║
 * ║  ┌───▼────┐         ┌──────▼───┐                                   ║
 * ║  │APPROVED│         │ REJECTED │                                   ║
 * ║  └────────┘         └──────────┘                                   ║
 * ║                                                                      ║
 * ║  BUSINESS RULES enforced at ClaimService level:                     ║
 * ║  • Only ADMIN or AGENT can move claim to UNDER_REVIEW, APPROVED,   ║
 * ║    or REJECTED                                                       ║
 * ║  • Once APPROVED or REJECTED, claim status is IMMUTABLE             ║
 * ║  • PENDING claims can be withdrawn by the CUSTOMER                  ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
public enum ClaimStatus {

    /**
     * Claim has been submitted by the customer and is awaiting review.
     * This is the initial state upon creation.
     */
    PENDING,

    /**
     * An agent or admin has started reviewing the claim.
     * The claim is no longer in the initial queue.
     */
    UNDER_REVIEW,

    /**
     * Claim has been approved. The insurance payout will be processed.
     * Terminal state — cannot transition to any other state.
     */
    APPROVED,

    /**
     * Claim has been rejected. The reviewNotes field explains the reason.
     * Terminal state — cannot transition to any other state.
     */
    REJECTED,

    /**
     * Customer withdrew the claim before it was reviewed.
     * Can only transition from PENDING state.
     */
    WITHDRAWN
}
