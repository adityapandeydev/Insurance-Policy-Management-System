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
import com.insurance.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
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
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData() {
        log.debug("Generating dashboard data");

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAgent = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_AGENT"));

        if (isAgent) {
            User user = (User) authentication.getPrincipal();
            Long agentId = user.getId();

            return DashboardResponse.builder()
                    // Customer metrics
                    .totalCustomers(customerRepository.countByAgentId(agentId))

                    // Policy metrics
                    .totalPolicies(policyRepository.countByCustomer_AgentId(agentId))
                    .activePolicies(policyRepository.countByCustomer_AgentIdAndStatus(agentId, PolicyStatus.ACTIVE))
                    .expiredPolicies(policyRepository.countByCustomer_AgentIdAndStatus(agentId, PolicyStatus.EXPIRED))
                    .pendingPolicies(policyRepository.countByCustomer_AgentIdAndStatus(agentId, PolicyStatus.PENDING))
                    .cancelledPolicies(policyRepository.countByCustomer_AgentIdAndStatus(agentId, PolicyStatus.CANCELLED))

                    // Claim metrics
                    .totalClaims(claimRepository.countByCustomer_AgentId(agentId))
                    .pendingClaims(claimRepository.countByCustomer_AgentIdAndStatus(agentId, ClaimStatus.PENDING))
                    .claimsUnderReview(claimRepository.countByCustomer_AgentIdAndStatus(agentId, ClaimStatus.UNDER_REVIEW))
                    .approvedClaims(claimRepository.countByCustomer_AgentIdAndStatus(agentId, ClaimStatus.APPROVED))
                    .rejectedClaims(claimRepository.countByCustomer_AgentIdAndStatus(agentId, ClaimStatus.REJECTED))

                    // Risk assessment metrics
                    .lowRiskCustomers(riskAssessmentRepository.countByCustomer_AgentIdAndRiskLevel(agentId, RiskLevel.LOW))
                    .mediumRiskCustomers(riskAssessmentRepository.countByCustomer_AgentIdAndRiskLevel(agentId, RiskLevel.MEDIUM))
                    .highRiskCustomers(riskAssessmentRepository.countByCustomer_AgentIdAndRiskLevel(agentId, RiskLevel.HIGH))

                    // User metrics (agents only care about their own counts)
                    .totalUsers(0)
                    .totalAgents(0)
                    .totalAdmins(0)

                    // Metadata
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

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
