package com.insurance.repository;

import com.insurance.entity.Claim;
import com.insurance.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Claim repository with customer-scoped and status-filtered queries.
 *
 * INTERVIEW TIP: N+1 Query Problem
 * ─────────────────────────────────
 * When loading a list of claims, if you access claim.getPolicy() for each one,
 * Hibernate fires N additional SQL queries (one per claim) to load the policy.
 * This is the "N+1 problem" — very common in JPA applications.
 *
 * Solution: @EntityGraph or JOIN FETCH to load related entities in one query.
 * @Query("SELECT c FROM Claim c JOIN FETCH c.policy WHERE c.customer.id = ?")
 * This produces a single JOIN query instead of N+1 queries.
 */
@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    /**
     * Lists all claims by a specific customer (paginated).
     * Used by: "My Claims" customer endpoint.
     */
    Page<Claim> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Lists claims by customer and status.
     * Used by: customer filtering "Show only PENDING claims".
     */
    Page<Claim> findByCustomerIdAndStatus(Long customerId, ClaimStatus status, Pageable pageable);

    /**
     * Lists all claims against a specific policy.
     * Used by: policy detail view showing claim history.
     */
    List<Claim> findByPolicyId(Long policyId);

    /**
     * Lists claims against a policy (paginated).
     */
    Page<Claim> findByPolicyId(Long policyId, Pageable pageable);

    /**
     * Dashboard: total claim count by status.
     */
    long countByStatus(ClaimStatus status);

    /**
     * Dashboard: total claims for a specific customer.
     */
    long countByCustomerId(Long customerId);

    /**
     * Finds a claim by its business-facing claim number.
     */
    java.util.Optional<Claim> findByClaimNumber(String claimNumber);

    /**
     * Checks if a claim with this number already exists (for uniqueness validation).
     */
    boolean existsByClaimNumber(String claimNumber);

    /**
     * Counts non-rejected/non-withdrawn claims for a policy.
     * Used in ClaimService to check active claim count before allowing new claims.
     * Business rule: a policy can have at most N active claims simultaneously.
     */
    @Query("""
        SELECT COUNT(c) FROM Claim c
        WHERE c.policy.id = :policyId
          AND c.status NOT IN ('REJECTED', 'WITHDRAWN')
        """)
    long countActiveClaimsForPolicy(@Param("policyId") Long policyId);

    /**
     * Calculates total approved claim amount for a policy.
     * Used in risk assessment: high payout history = higher risk score.
     *
     * COALESCE handles the case where there are no approved claims (SUM returns NULL).
     */
    @Query("""
        SELECT COALESCE(SUM(c.claimAmount), 0)
        FROM Claim c
        WHERE c.policy.id = :policyId AND c.status = 'APPROVED'
        """)
    BigDecimal sumApprovedClaimAmountForPolicy(@Param("policyId") Long policyId);

    /**
     * Admin: get all claims with filtering support.
     */
    Page<Claim> findAll(Pageable pageable);

    /**
     * Count all claims for a given customer (for risk assessment).
     */
    @Query("SELECT COUNT(c) FROM Claim c WHERE c.customer.id = :customerId AND c.status = 'APPROVED'")
    long countApprovedClaimsByCustomer(@Param("customerId") Long customerId);
}
