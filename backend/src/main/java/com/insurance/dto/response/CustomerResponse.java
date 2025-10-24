package com.insurance.dto.response;

import com.insurance.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for customer data returned to clients.
 *
 * INTERVIEW TIP: Response DTOs control what data is exposed via the API.
 * We include: all safe customer fields + denormalized user fields (name, email)
 * We EXCLUDE: password, internal JPA metadata, lazy-loaded collections
 *
 * Notice: We do NOT include List<PolicyResponse> or List<ClaimResponse> here.
 * Loading nested collections causes N+1 queries.
 * Instead, the frontend calls /policies?customerId=X separately (better for pagination).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {

    // Customer fields
    private Long id;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private String nationalId;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String occupation;
    private int age;    // calculated from dateOfBirth

    // Denormalized from User entity (avoids nested UserResponse object)
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;

    // Summary counts (loaded via separate count queries, not lazy collections)
    private long totalPolicies;
    private long totalClaims;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
