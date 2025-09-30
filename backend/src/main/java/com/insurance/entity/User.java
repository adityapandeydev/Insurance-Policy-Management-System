package com.insurance.entity;

import com.insurance.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                          USER ENTITY                                    ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  DUAL ROLE: This class serves two purposes simultaneously:              ║
 * ║  1. JPA Entity → mapped to the 'users' database table                  ║
 * ║  2. Spring Security UserDetails → used for authentication              ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: UserDetails Interface                                   ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Spring Security's authentication pipeline works like this:            ║
 * ║  HTTP Request → JwtAuthFilter → loads UserDetails from DB              ║
 * ║               → validates JWT → creates Authentication object          ║
 * ║               → stores in SecurityContextHolder                        ║
 * ║                                                                          ║
 * ║  UserDetails provides 4 boolean methods Spring Security uses:           ║
 * ║  • isAccountNonExpired()  → Is the account still valid?                ║
 * ║  • isAccountNonLocked()   → Is the account not locked?                 ║
 * ║  • isCredentialsNonExpired() → Is the password still valid?            ║
 * ║  • isEnabled()            → Is the account active?                     ║
 * ║                                                                          ║
 * ║  JPA ANNOTATIONS EXPLAINED:                                             ║
 * ║  @Entity → Tells JPA this class maps to a database table               ║
 * ║  @Table(name="users") → Specifies the exact table name                 ║
 * ║  @Id → Marks the primary key field                                      ║
 * ║  @GeneratedValue(IDENTITY) → DB auto-increments the ID (BIGSERIAL)     ║
 * ║  @Column → Customizes column mapping (name, nullable, unique, length)  ║
 * ║  @Enumerated(STRING) → Stores enum as human-readable string             ║
 * ║  @OneToOne → One user has one customer profile                          ║
 * ║                                                                          ║
 * ║  LOMBOK ANNOTATIONS:                                                    ║
 * ║  @Getter, @Setter → auto-generates getters and setters                 ║
 * ║  @NoArgsConstructor → generates no-arg constructor (required by JPA)   ║
 * ║  @AllArgsConstructor → generates full constructor (used with @Builder) ║
 * ║  @Builder → enables User.builder().email("...").build() pattern        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        // UNIQUE constraint at JPA level (mirrors DB constraint)
        // This causes Hibernate to validate schema matches entity mapping
        @UniqueConstraint(columnNames = "email", name = "uk_users_email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    /**
     * @Id: Marks this as the primary key
     * @GeneratedValue(IDENTITY): Delegates ID generation to the database.
     * PostgreSQL uses BIGSERIAL (auto-increment). The DB generates the ID
     * on INSERT, then Hibernate reads it back.
     *
     * INTERVIEW TIP: IDENTITY strategy relies on DB auto-increment.
     * SEQUENCE strategy uses a DB sequence (more efficient for batch inserts
     * because Hibernate can pre-fetch sequence values without hitting DB).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @Column: Customizes the column mapping.
     * nullable = false → NOT NULL constraint in DB schema
     * unique = true → UNIQUE constraint (for fast login lookup)
     * length = 255 → VARCHAR(255)
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Stores BCrypt-hashed password.
     * BCrypt format: $2a$10$<22-char-salt><31-char-hash>
     * Total length is always 60 characters.
     *
     * INTERVIEW TIP: BCrypt is adaptive — the cost factor (10 above)
     * controls computation time. As hardware improves, you increase the
     * cost factor to keep brute-forcing impractical.
     * bcrypt(password) → always different even for same input (random salt)
     * BCryptPasswordEncoder.matches(raw, hash) → validates without storing raw
     */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * @Enumerated(EnumType.STRING): Stores the enum name as a VARCHAR.
     * Stores "ROLE_ADMIN", "ROLE_AGENT", "ROLE_CUSTOMER" — not 0, 1, 2.
     *
     * WHY STRING not ORDINAL?
     * If you add/reorder enum values, ORDINAL breaks existing data.
     * STRING is stable — order changes don't affect stored values.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    /**
     * Soft-delete / account suspension flag.
     * Instead of deleting users (which breaks FK references),
     * we set enabled=false. The user can no longer log in.
     *
     * @Builder.Default: Lombok's @Builder ignores field initializers by default.
     * This annotation makes the builder use 'true' as the default value.
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * @OneToOne: Maps to the Customer entity.
     * mappedBy = "user" → Customer owns the FK (customer.user_id)
     * cascade = ALL → Save/delete user cascades to customer profile
     * fetch = LAZY → Customer data loaded only when accessed (performance)
     *
     * INTERVIEW TIP: FetchType.LAZY vs EAGER
     * LAZY: SQL loads User only; Customer loaded separately when you access it
     * EAGER: Both User and Customer are JOINed in a single SQL query
     * Default for @OneToOne is EAGER (always loads both) — we override to LAZY
     * for performance (most auth operations don't need the Customer profile).
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Customer customer;

    /**
     * @CreationTimestamp: Hibernate sets this to NOW() when entity is first inserted.
     * updatable = false → Hibernate never updates this column after initial insert.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * @UpdateTimestamp: Hibernate updates this to NOW() on every UPDATE.
     * Automatically maintained — no manual code needed.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ════════════════════════════════════════════════════════════════════════
    // UserDetails Interface Implementation
    // ════════════════════════════════════════════════════════════════════════
    // Spring Security calls these methods during authentication and authorization.

    /**
     * Returns the authorities (roles/permissions) granted to the user.
     * SimpleGrantedAuthority wraps a role string like "ROLE_ADMIN".
     *
     * Spring Security checks this via:
     * @PreAuthorize("hasRole('ADMIN')") → looks for "ROLE_ADMIN" in authorities
     * .hasAuthority("ROLE_ADMIN")       → same as above, just explicit
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // We use a single role per user. For multi-role systems, this would
        // return a list mapped from a roles collection.
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Returns the password used for authentication.
     * Spring Security's DaoAuthenticationProvider calls this during login
     * and compares it with the raw password using PasswordEncoder.matches().
     */
    @Override
    public String getPassword() {
        return password;
    }

    /**
     * Returns the username used for authentication.
     * In our system, the email address serves as the username.
     * Spring Security uses this to load the user in UserDetailsService.loadUserByUsername().
     */
    @Override
    public String getUsername() {
        return email;
    }

    // The following 3 methods all return true to keep the logic simple.
    // In a production system, you'd add accountExpired, accountLocked, credentialsExpired fields.

    @Override
    public boolean isAccountNonExpired() {
        return true;  // Account never expires based on time
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  // No account locking mechanism (could add failed attempt tracking)
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // Passwords don't expire (could add password reset cycles)
    }

    @Override
    public boolean isEnabled() {
        return enabled;  // Controlled by the 'enabled' field (soft-delete)
    }

    /**
     * Convenience method: returns the user's full name.
     * Used in logging, email templates, etc.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
