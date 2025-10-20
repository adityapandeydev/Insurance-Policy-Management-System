package com.insurance.dto.response;

import com.insurance.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO returned after successful registration or login.
 * Contains the JWT token and basic user information.
 *
 * The client stores the token and sends it in:
 * Authorization: Bearer <token>
 * for all subsequent authenticated requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** JWT access token — sent by client in Authorization header */
    private String accessToken;

    /** Token type — always "Bearer" for JWT */
    @Builder.Default
    private String tokenType = "Bearer";

    /** Token validity period in milliseconds */
    private long expiresIn;

    /** Authenticated user details */
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
}
