package com.insurance.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO for creating or updating a customer profile.
 *
 * INTERVIEW TIP: Why separate DTOs for create vs update?
 * ──────────────────────────────────────────────────────
 * Some fields required on create may be optional on update (PATCH).
 * Some fields should never be updated (e.g., national ID for KYC).
 * Separate DTOs give you fine-grained control over what's allowed when.
 * For this project, we use a single request DTO for simplicity,
 * but mark non-updateable fields clearly in the service layer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be 10-15 digits, optionally starting with +")
    private String phoneNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 50, message = "National ID must not exceed 50 characters")
    private String nationalId;

    private String emergencyContactName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Emergency contact phone must be 10-15 digits")
    private String emergencyContactPhone;

    private String occupation;
}
