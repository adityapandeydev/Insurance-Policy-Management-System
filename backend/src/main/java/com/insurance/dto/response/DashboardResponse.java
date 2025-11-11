package com.insurance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for admin dashboard analytics.
 * Aggregates data from all modules into a single response.
 *
 * INTERVIEW TIP: Aggregation at the service layer vs DB layer
 * ─────────────────────────────────────────────────────────────
 * This DTO is populated by DashboardService which makes multiple
 * targeted COUNT queries (one per metric). An alternative is to use
 * a single native SQL query with multiple COUNT aggregations.
 * The multi-query approach is simpler but less efficient for large datasets.
 * For a production system with high load, a materialized view or
 * caching layer (Redis) would be used for dashboard metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    // Customer metrics
    private long totalCustomers;

    // Policy metrics
    private long totalPolicies;
    private long activePolicies;
    private long expiredPolicies;
    private long pendingPolicies;
    private long cancelledPolicies;

    // Claim metrics
    private long totalClaims;
    private long pendingClaims;
    private long claimsUnderReview;
    private long approvedClaims;
    private long rejectedClaims;

    // Risk assessment metrics
    private long lowRiskCustomers;
    private long mediumRiskCustomers;
    private long highRiskCustomers;

    // User metrics
    private long totalUsers;
    private long totalAgents;
    private long totalAdmins;

    // When was this dashboard data generated?
    private LocalDateTime generatedAt;
}
