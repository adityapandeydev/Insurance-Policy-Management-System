package com.insurance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) for user registration request.
 *
 * INTERVIEW TIP: Why use DTOs instead of exposing Entity directly?
 * ─────────────────────────────────────────────────────────────────
 * 1. Security: Prevents mass assignment attacks (Jackson would bind ALL fields
 *    on the entity if you accept entity directly, including internal ones)
 * 2. Validation: Entities shouldn't carry request validation annotations
 *    (validation is a presentation concern, not a domain concern)
 * 3. API stability: Entity can evolve without breaking the API contract
 * 4. Reduced payload: DTOs expose only relevant fields (not all entity fields)
 *
 * @Data (Lombok) = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
}
