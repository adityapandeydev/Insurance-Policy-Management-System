package com.insurance.service;

import com.insurance.dto.request.LoginRequest;
import com.insurance.dto.request.RegisterRequest;
import com.insurance.dto.response.AuthResponse;
import com.insurance.entity.User;
import com.insurance.enums.Role;
import com.insurance.exception.BusinessRuleException;
import com.insurance.repository.UserRepository;
import com.insurance.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                        AUTHENTICATION SERVICE                           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Handles user registration and login (JWT-based authentication).        ║
 * ║                                                                          ║
 * ║  REGISTRATION FLOW:                                                     ║
 * ║  1. Validate email uniqueness                                           ║
 * ║  2. Hash password with BCrypt                                           ║
 * ║  3. Save user to DB                                                     ║
 * ║  4. Generate JWT token                                                  ║
 * ║  5. Return AuthResponse with token + user info                         ║
 * ║                                                                          ║
 * ║  LOGIN FLOW:                                                            ║
 * ║  1. Call AuthenticationManager.authenticate(email, rawPassword)         ║
 * ║  2. Spring Security: loads user from DB, BCrypt.matches(raw, stored)    ║
 * ║  3. If match: returns authenticated auth token                         ║
 * ║  4. Generate JWT token                                                  ║
 * ║  5. Return AuthResponse                                                 ║
 * ║                                                                          ║
 * ║  @Transactional                                                         ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Wraps the method in a database transaction.                            ║
 * ║  If any step fails, ALL changes are rolled back (atomicity).           ║
 * ║  Prevents partial writes (e.g., user saved but token generation fails). ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Registers a new user with the CUSTOMER role.
     *
     * INTERVIEW TIP: Why is the role hardcoded to ROLE_CUSTOMER here?
     * Security principle: users should not be able to self-assign elevated roles.
     * Admin/Agent accounts are created by an existing ADMIN through a separate endpoint.
     * This prevents privilege escalation via the public registration endpoint.
     *
     * @param request Registration details (firstName, lastName, email, password)
     * @return AuthResponse containing JWT token and user info
     * @throws BusinessRuleException if email is already registered
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Attempting registration for email: {}", request.getEmail());

        // ─── Step 1: Check email uniqueness ─────────────────────────────────
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException(
                    "An account with email '" + request.getEmail() + "' already exists."
            );
        }

        // ─── Step 2: Build and save the User entity ──────────────────────────
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase().trim())
                // BCrypt: hash the raw password — NEVER store plaintext
                .password(passwordEncoder.encode(request.getPassword()))
                // New registrations are always CUSTOMER role
                .role(Role.ROLE_CUSTOMER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with id: {} and email: {}",
                savedUser.getId(), savedUser.getEmail());

        // ─── Step 3: Generate JWT token ──────────────────────────────────────
        // Add custom claims to the token (role for frontend authorization decisions)
        Map<String, Object> extraClaims = buildExtraClaims(savedUser);
        String jwtToken = jwtService.generateToken(extraClaims, savedUser);

        return buildAuthResponse(savedUser, jwtToken);
    }

    /**
     * Authenticates an existing user and returns a JWT token.
     *
     * INTERVIEW TIP: We call authenticationManager.authenticate() which:
     * 1. Delegates to DaoAuthenticationProvider
     * 2. DaoAuthenticationProvider calls loadUserByUsername(email)
     * 3. Compares raw password with BCrypt hash: passwordEncoder.matches(raw, hash)
     * 4. Returns authenticated token if successful
     * 5. Throws BadCredentialsException (→ 401) if passwords don't match
     *
     * @param request Login credentials (email, password)
     * @return AuthResponse with JWT token
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // ─── Step 1: Authenticate (Spring Security handles password verification) ──
        // This call EITHER returns an authenticated token OR throws an exception.
        // BadCredentialsException → GlobalExceptionHandler catches it → 401 response
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // ─── Step 2: Get the authenticated User from the Authentication object ──
        // After successful authentication, the principal is our UserDetails (User entity)
        User user = (User) authentication.getPrincipal();

        log.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole());

        // ─── Step 3: Generate JWT token ──────────────────────────────────────
        Map<String, Object> extraClaims = buildExtraClaims(user);
        String jwtToken = jwtService.generateToken(extraClaims, user);

        return buildAuthResponse(user, jwtToken);
    }

    /**
     * Creates a map of extra claims to embed in the JWT payload.
     * Frontend uses these claims for UI decisions (e.g., show/hide admin menu).
     *
     * INTERVIEW TIP: Keep JWT payload small. Include only essential info
     * needed by the client for authorization decisions. Large payloads
     * increase bandwidth usage (sent in every HTTP header).
     */
    private Map<String, Object> buildExtraClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        claims.put("fullName", user.getFullName());
        return claims;
    }

    /**
     * Builds the standardized AuthResponse DTO from a User entity and JWT token.
     */
    private AuthResponse buildAuthResponse(User user, String jwtToken) {
        return AuthResponse.builder()
                .accessToken(jwtToken)
                .tokenType("Bearer")
                .expiresIn(jwtExpiration)
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .build();
    }
}
