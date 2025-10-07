package com.insurance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                        CUSTOMER ENTITY                                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Represents a customer's insurance profile — the business domain        ║
 * ║  object that holds personal, contact, and demographic information.       ║
 * ║                                                                          ║
 * ║  RELATIONSHIPS:                                                          ║
 * ║  User       (1) ─── (1) Customer   [@OneToOne, FK on customer side]     ║
 * ║  Customer   (1) ─── (N) Policy     [@OneToMany, FK on policy side]      ║
 * ║  Customer   (1) ─── (N) Claim      [@OneToMany, FK on claim side]       ║
 * ║  Customer   (1) ─── (1) RiskAssess [@OneToOne, FK on risk side]         ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: JPA Relationship Ownership                              ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  The "owning side" of a relationship holds the FK column in the DB.     ║
 * ║  @JoinColumn appears on the OWNING side.                                ║
 * ║  mappedBy appears on the INVERSE (non-owning) side.                    ║
 * ║                                                                          ║
 * ║  For @OneToMany: The "Many" side owns the FK.                          ║
 * ║  Customer.policies → mappedBy = "customer" (Policy owns the FK)        ║
 * ║  Policy.customer → @JoinColumn (Policy.customer_id is the FK column)   ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Entity
@Table(
    name = "customers",
    indexes = {
        @Index(name = "idx_customers_user_id", columnList = "user_id"),
        @Index(name = "idx_customers_phone", columnList = "phone_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @OneToOne: One Customer is linked to exactly one User.
     * @JoinColumn: Customer table has the FK column "user_id".
     *   → This means Customer is the OWNING side of the relationship.
     * fetch = LAZY: User is NOT loaded with Customer by default.
     *   We load User separately only when needed (e.g., for email).
     *
     * INTERVIEW TIP: @JoinColumn(name="user_id") tells Hibernate the exact
     * column name in the DB. Without it, Hibernate generates a name like
     * "user_id" anyway, but explicit is always better.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ─── CONTACT INFORMATION ─────────────────────────────────────────────
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    // ─── DEMOGRAPHIC DATA ─────────────────────────────────────────────────
    /**
     * Date of birth: Stored as LocalDate (date only, no time component).
     * Used in RiskAssessmentService to calculate customer age.
     * JPA maps LocalDate to DATE type in PostgreSQL.
     */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * National ID (Aadhaar, SSN, NIN etc.)
     * UNIQUE constraint: one national ID per customer.
     * Nullable: optional for customers who haven't provided KYC yet.
     */
    @Column(name = "national_id", unique = true, length = 50)
    private String nationalId;

    @Column(name = "emergency_contact_name", length = 200)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(length = 100)
    private String occupation;

    // ─── RELATIONSHIPS ────────────────────────────────────────────────────

    /**
     * @OneToMany: One Customer has many Policies.
     * mappedBy = "customer" → Policy entity has a @ManyToOne field named "customer"
     *   which owns the FK column. Customer is the INVERSE side.
     *
     * cascade = ALL → Saving customer saves its policies; deleting customer deletes policies.
     * orphanRemoval = true → If you remove a Policy from this list and save,
     *   the Policy record is deleted from DB (treated as an "orphan").
     *
     * fetch = LAZY → Policies NOT loaded when you load a Customer.
     *   You must explicitly call customer.getPolicies() to trigger the SQL.
     *   This avoids loading hundreds of policies when you just need the customer's name.
     *
     * @Builder.Default → Initializes the list to avoid NullPointerExceptions
     *   when using the builder pattern without setting policies.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Policy> policies = new ArrayList<>();

    /**
     * @OneToMany: One Customer has many Claims.
     * Same pattern as policies above.
     * Stored separately for quick access to "all claims by this customer"
     * without going through Policy.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<Claim> claims = new ArrayList<>();

    /**
     * @OneToOne: One Customer has one RiskAssessment profile.
     * mappedBy = "customer" → RiskAssessment holds the FK.
     * cascade = ALL → Deleting customer deletes their risk profile.
     */
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL,
              fetch = FetchType.LAZY)
    private RiskAssessment riskAssessment;

    // ─── AUDIT FIELDS ─────────────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── HELPER METHODS ───────────────────────────────────────────────────

    /**
     * Calculates the customer's age in years from date of birth.
     * Used by RiskAssessmentService for age scoring.
     *
     * @return age in years, or 0 if dateOfBirth is null
     */
    public int getAge() {
        if (dateOfBirth == null) return 0;
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    /**
     * Convenience method to get the customer's full name via the linked User.
     */
    public String getFullName() {
        return user != null ? user.getFullName() : "Unknown";
    }

    /**
     * Convenience method to get the customer's email via the linked User.
     */
    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }
}
