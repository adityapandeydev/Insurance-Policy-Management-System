package com.insurance.repository;

import com.insurance.entity.User;
import com.insurance.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                        USER REPOSITORY                                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Spring Data JPA repository for User entity.                            ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: How Spring Data JPA Works                               ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  JpaRepository<User, Long> provides out-of-the-box methods:            ║
 * ║  • save(entity)         → INSERT or UPDATE                              ║
 * ║  • findById(id)         → SELECT WHERE id = ?                           ║
 * ║  • findAll()            → SELECT * FROM users                           ║
 * ║  • findAll(Pageable)    → SELECT with LIMIT, OFFSET, ORDER BY          ║
 * ║  • delete(entity)       → DELETE WHERE id = ?                           ║
 * ║  • count()              → SELECT COUNT(*) FROM users                    ║
 * ║                                                                          ║
 * ║  DERIVED QUERY METHODS:                                                  ║
 * ║  Spring parses method names and generates SQL automatically:            ║
 * ║  findByEmail(email) → SELECT * FROM users WHERE email = ?              ║
 * ║  findByRole(role)   → SELECT * FROM users WHERE role = ?               ║
 * ║  existsByEmail(e)   → SELECT COUNT(*) > 0 WHERE email = ?              ║
 * ║                                                                          ║
 * ║  Naming convention: find|count|exists + By + FieldName + [And|Or|...] ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Repository  // Marks this as a Spring-managed repository; enables exception translation
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * Used by: UserDetailsService (authentication), AuthService (duplicate check).
     *
     * Derived query → Hibernate generates:
     * SELECT * FROM users WHERE email = ?
     *
     * Returns Optional<User> to avoid returning null (safer null handling).
     * Caller must handle the case where user doesn't exist.
     *
     * @param email The email address to search for
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user with the given email already exists.
     * Used in AuthService.register() to enforce unique email constraint.
     *
     * More efficient than findByEmail() for existence checks:
     * → SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * Does not load the full User object (saves memory and network).
     *
     * @param email The email to check
     * @return true if a user with this email exists
     */
    boolean existsByEmail(String email);

    /**
     * Counts users by role.
     * Used by DashboardService for admin analytics.
     *
     * Derived query → SELECT COUNT(*) FROM users WHERE role = ?
     *
     * @param role The role to count
     * @return Number of users with this role
     */
    long countByRole(Role role);

    /**
     * Custom JPQL query using @Query.
     * Used when derived query naming becomes complex or unclear.
     *
     * INTERVIEW TIP: JPQL vs SQL
     * • JPQL operates on Entity class names and field names (not table/column names)
     * • Hibernate translates JPQL → SQL for the target database dialect
     * • This makes code database-agnostic (same JPQL works for PostgreSQL, MySQL, H2)
     * • Native SQL (@Query with nativeQuery=true) is database-specific
     *
     * @param email The email to search for
     * @return Optional<User>
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<User> findByEmailIgnoreCase(String email);
}
