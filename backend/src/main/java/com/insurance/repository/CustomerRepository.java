package com.insurance.repository;

import com.insurance.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Customer repository with pagination and search support.
 *
 * INTERVIEW TIP: Pagination in Spring Data JPA
 * ─────────────────────────────────────────────
 * Spring Data JPA supports pagination out of the box via the Pageable interface.
 * Instead of returning List<T>, return Page<T> or Slice<T>.
 *
 * Page<T> contains:
 * • content      → List of entities for current page
 * • totalElements → Total count (needs COUNT query)
 * • totalPages   → totalElements / pageSize
 * • number       → Current page number (0-indexed)
 * • size         → Page size
 * • first/last   → Is this the first/last page?
 *
 * Usage: repository.findAll(PageRequest.of(0, 10, Sort.by("createdAt").descending()))
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Finds a customer by their linked User's ID.
     * Used by: CustomerService (when CUSTOMER role accesses their own profile).
     *
     * JPQL JOIN: Navigates entity associations using dot notation.
     * "c.user.id" traverses: Customer → User → id
     * Hibernate generates: SELECT c.* FROM customers c WHERE c.user_id = ?
     */
    @Query("SELECT c FROM Customer c WHERE c.user.id = :userId")
    Optional<Customer> findByUserId(@Param("userId") Long userId);

    /**
     * Finds a customer by their linked User's email.
     * Spring Data derives: JOIN users u ON c.user_id = u.id WHERE u.email = ?
     */
    Optional<Customer> findByUserEmail(String email);

    /**
     * Checks if a customer with given national ID already exists.
     * Used for KYC validation during customer creation.
     */
    boolean existsByNationalId(String nationalId);

    /**
     * Paginated search across customer name, email, and phone.
     *
     * @Query with JPQL:
     * • LOWER() for case-insensitive search
     * • LIKE with % wildcard for partial matching
     * • OR across multiple fields
     *
     * INTERVIEW TIP: For complex search, consider Spring Specification or QueryDSL.
     * @Query is fine for simple search. Specifications allow dynamic where clauses.
     *
     * @param searchTerm Partial name, email, or phone number
     * @param pageable   Pagination and sorting configuration
     * @return Page of matching customers
     */
    @Query("""
        SELECT c FROM Customer c
        WHERE (:agentId IS NULL OR c.agent.id = :agentId)
        AND (LOWER(c.user.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(c.user.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR LOWER(c.user.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           OR c.phoneNumber LIKE CONCAT('%', :searchTerm, '%'))
        """)
    Page<Customer> searchCustomers(@Param("searchTerm") String searchTerm, @Param("agentId") Long agentId, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE (:agentId IS NULL OR c.agent.id = :agentId)")
    Page<Customer> findAllByAgentId(@Param("agentId") Long agentId, Pageable pageable);

    java.util.List<Customer> findAllByAgentIdIsNull();

    long countByAgentId(Long agentId);

    /**
     * Returns total count of customers.
     * Inherited from JpaRepository: count() → SELECT COUNT(*) FROM customers
     * Used by DashboardService.
     */

    /**
     * Paginated list of all customers.
     * Inherited from JpaRepository: findAll(Pageable pageable)
     */
}
