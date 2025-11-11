package com.insurance.service;

import com.insurance.dto.response.DashboardResponse;
import com.insurance.enums.ClaimStatus;
import com.insurance.enums.PolicyStatus;
import com.insurance.enums.RiskLevel;
import com.insurance.enums.Role;
import com.insurance.repository.ClaimRepository;
import com.insurance.repository.CustomerRepository;
import com.insurance.repository.PolicyRepository;
import com.insurance.repository.RiskAssessmentRepository;
import com.insurance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                       DASHBOARD SERVICE                                 ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Aggregates metrics from all repositories into a single dashboard.      ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: Performance considerations for dashboards               ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  This implementation makes ~10 COUNT queries per request.               ║
 * ║  For a production system with millions of records:                      ║
 * ║  Option 1: Cache with @Cacheable (Spring Cache + Redis)                ║
 * ║            → Dashboard updates every 5 minutes, not per request        ║
 * ║  Option 2: Materialized views in PostgreSQL                             ║
 * ║            → Pre-computed aggregates, refreshed on schedule            ║
 * ║  Option 3: Dedicated reporting database (OLAP)                         ║
 * ║            → Separate read-optimized DB for analytics queries          ║
 * ║  For this scale (thousands of records), COUNT queries are fine.        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final UserRepository userRepository;

    /**
     * Aggregates all system metrics for the admin dashboard.
     *
     * @Transactional(readOnly = true): All queries in this method are read-only.
     * readOnly=true: hint to DB to use read-only transaction mode:
     * → PostgreSQL: can use read replicas
     * → No dirty-checking overhead (Hibernate doesn't track entity changes)
     * → No flush required at end of transaction
     *
     * @return DashboardResponse with all aggregated metrics
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        log.debug("Generating dashboard data");

        return DashboardResponse.builder()
                // Customer metrics
                .totalCustomers(customerRepository.count())

                // Policy metrics
                .totalPolicies(policyRepository.count())
                .activePolicies(policyRepository.countByStatus(PolicyStatus.ACTIVE))
                .expiredPolicies(policyRepository.countByStatus(PolicyStatus.EXPIRED))
                .pendingPolicies(policyRepository.countByStatus(PolicyStatus.PENDING))
                .cancelledPolicies(policyRepository.countByStatus(PolicyStatus.CANCELLED))

                // Claim metrics
                .totalClaims(claimRepository.count())
                .pendingClaims(claimRepository.countByStatus(ClaimStatus.PENDING))
                .claimsUnderReview(claimRepository.countByStatus(ClaimStatus.UNDER_REVIEW))
                .approvedClaims(claimRepository.countByStatus(ClaimStatus.APPROVED))
                .rejectedClaims(claimRepository.countByStatus(ClaimStatus.REJECTED))

                // Risk assessment metrics
                .lowRiskCustomers(riskAssessmentRepository.countByRiskLevel(RiskLevel.LOW))
                .mediumRiskCustomers(riskAssessmentRepository.countByRiskLevel(RiskLevel.MEDIUM))
                .highRiskCustomers(riskAssessmentRepository.countByRiskLevel(RiskLevel.HIGH))

                // User metrics
                .totalUsers(userRepository.count())
                .totalAgents(userRepository.countByRole(Role.ROLE_AGENT))
                .totalAdmins(userRepository.countByRole(Role.ROLE_ADMIN))

                // Metadata
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
