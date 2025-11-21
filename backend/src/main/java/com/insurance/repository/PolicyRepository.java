package com.insurance.repository;

import com.insurance.entity.Policy;
import com.insurance.enums.PolicyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Policy repository with status filtering, pagination, and bulk update support.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /**
     * Finds all policies for a specific customer (paginated).
     * Used by customer-facing "My Policies" endpoint.
     *
     * Derived query: finds by customer's ID field.
     */
    Page<Policy> findByCustomerId(Long customerId, Pageable pageable);

    /**
     * Finds all policies for a customer with a specific status.
     * Used to show "active policies" or "expired policies" for a customer.
     */
    Page<Policy> findByCustomerIdAndStatus(Long customerId, PolicyStatus status, Pageable pageable);

    /**
     * Finds a policy by its business-facing policy number.
     * Used for policy lookup by customers/agents.
     */
    Optional<Policy> findByPolicyNumber(String policyNumber);

    /**
     * Dashboard: counts policies by status.
     * SELECT COUNT(*) FROM policies WHERE status = ?
     */
    long countByStatus(PolicyStatus status);

    /**
     * Counts policies by customer (used in customer detail view).
     */
    long countByCustomerId(Long customerId);

    long countByCustomer_AgentId(Long agentId);
    long countByCustomer_AgentIdAndStatus(Long agentId, PolicyStatus status);

    /**
     * Searches policies by policy number, name, or customer name.
     * Supports paginated results for the admin policy listing.
     */
    @Query("""
        SELECT p FROM Policy p
        WHERE (:agentId IS NULL OR p.customer.agent.id = :agentId)
        AND (LOWER(p.policyNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(p.policyName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(p.customer.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(p.customer.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        """)
    Page<Policy> searchPolicies(@Param("searchTerm") String searchTerm, @Param("agentId") Long agentId, Pageable pageable);

    @Query("SELECT p FROM Policy p WHERE (:agentId IS NULL OR p.customer.agent.id = :agentId)")
    Page<Policy> findAllByAgentId(@Param("agentId") Long agentId, Pageable pageable);

    /**
     * Finds all ACTIVE policies whose end date has passed today.
     * Used by the @Scheduled policy expiry job to bulk-expire overdue policies.
     *
     * INTERVIEW TIP: @Modifying annotation
     * Required for UPDATE and DELETE JPQL queries. Without @Modifying,
     * Hibernate treats the query as read-only and throws an exception.
     * @Transactional is also required on the calling service method.
     */
    @Query("SELECT p FROM Policy p WHERE p.status = 'ACTIVE' AND p.endDate < :today")
    List<Policy> findActivePoliciesExpiredBefore(@Param("today") LocalDate today);

    /**
     * Bulk update ACTIVE policies to EXPIRED status where end date has passed.
     *
     * @Modifying: marks this as a write operation (UPDATE/DELETE)
     * clearAutomatically = true: clears the first-level cache (EntityManager)
     *   after the bulk update, so subsequent reads see fresh data from DB.
     *
     * INTERVIEW TIP: Why clear the EntityManager cache after bulk updates?
     * Bulk JPQL updates bypass the EntityManager's first-level cache.
     * Without clearAutomatically=true, subsequent reads may return stale
     * cached entities instead of the updated DB data.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Policy p SET p.status = 'EXPIRED' WHERE p.status = 'ACTIVE' AND p.endDate < :today")
    int expireActivePolicies(@Param("today") LocalDate today);

    /**
     * Finds all policies for a specific customer without pagination.
     * Used by ClaimService to validate customer policy ownership.
     */
    List<Policy> findByCustomerId(Long customerId);
}
