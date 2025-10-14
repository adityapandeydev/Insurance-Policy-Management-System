package com.insurance.config;

import com.insurance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                     APPLICATION CONFIGURATION                           ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  This class defines core infrastructure beans needed across the app.    ║
 * ║  Kept separate from SecurityConfig to avoid circular dependency issues. ║
 * ║                                                                          ║
 * ║  BEANS DEFINED:                                                          ║
 * ║  1. UserDetailsService — loads users from DB by email                  ║
 * ║  2. AuthenticationProvider — wires UserDetailsService + PasswordEncoder ║
 * ║  3. AuthenticationManager — entry point for authentication             ║
 * ║  4. PasswordEncoder (BCrypt) — hashes and verifies passwords           ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: @Configuration vs @Component                           ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  @Configuration indicates this class is a source of @Bean definitions. ║
 * ║  Spring uses CGLIB proxying to ensure @Bean methods return the SAME    ║
 * ║  singleton instance even when called multiple times within the class.  ║
 * ║  @Component does not have this guarantee (no proxy by default).        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    /**
     * UserDetailsService Bean.
     *
     * This is defined as a lambda (functional interface) since UserDetailsService
     * has only one abstract method: loadUserByUsername(String username).
     *
     * INTERVIEW TIP: By defining this as a @Bean in @Configuration, Spring
     * uses it to wire the DaoAuthenticationProvider below. If we had defined
     * UserDetailsServiceImpl separately and also had this bean, there would be
     * ambiguity — we avoid that by having one canonical UserDetailsService bean.
     *
     * @return UserDetailsService that loads users from PostgreSQL by email
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username
                ));
    }

    /**
     * AuthenticationProvider Bean.
     *
     * DaoAuthenticationProvider is Spring Security's standard implementation
     * that uses a UserDetailsService and PasswordEncoder to authenticate users.
     *
     * HOW IT WORKS:
     * 1. Receives a UsernamePasswordAuthenticationToken from AuthenticationManager
     * 2. Calls userDetailsService.loadUserByUsername(username) → loads User from DB
     * 3. Calls passwordEncoder.matches(providedPassword, storedHashedPassword)
     * 4. If passwords match → returns authenticated Authentication object
     * 5. If not → throws BadCredentialsException → 401 Unauthorized
     *
     * INTERVIEW TIP: The AuthenticationProvider pattern is extensible.
     * You can have multiple providers: DaoAuthenticationProvider (DB),
     * LdapAuthenticationProvider (LDAP/AD), OAuth2LoginAuthenticationProvider.
     * Spring tries each provider in order until one succeeds or all fail.
     *
     * @return Configured DaoAuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        // Wire our UserDetailsService (DB-backed user lookup)
        authProvider.setUserDetailsService(userDetailsService());

        // Wire BCrypt password encoder for password verification
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    /**
     * AuthenticationManager Bean.
     *
     * The AuthenticationManager is the entry point for Spring Security's
     * authentication process. It delegates to registered AuthenticationProviders.
     *
     * In our AuthService, we call:
     * authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))
     *
     * INTERVIEW TIP: Why get it from AuthenticationConfiguration?
     * Spring Boot auto-configures the AuthenticationManager via
     * AuthenticationConfiguration. Extracting it from there ensures we get
     * the same instance Spring uses internally — avoids circular dependency
     * issues that arise from manually constructing it.
     *
     * @param config Spring's AuthenticationConfiguration
     * @return The configured AuthenticationManager
     * @throws Exception if configuration fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt Password Encoder Bean.
     *
     * BCryptPasswordEncoder uses the BCrypt adaptive hashing algorithm.
     *
     * KEY PROPERTIES:
     * 1. Adaptive: The cost factor (strength) can be increased as hardware improves
     * 2. Salt included: Each hash includes a random 22-character salt
     *    → Same password always produces a different hash → rainbow table attacks useless
     * 3. Slow by design: Takes ~100ms per hash (prevents brute-force attacks)
     *
     * INTERVIEW TIP:
     * BCrypt hash format: $2a$10$<22-char-salt><31-char-hash>
     *   $2a = BCrypt version
     *   $10 = cost factor (2^10 = 1024 iterations)
     *   salt = 22 Base64 characters
     *   hash = 31 Base64 characters
     * Total: always 60 characters
     *
     * Verification: BCryptPasswordEncoder.matches(rawPassword, storedHash)
     * → Extracts salt from stored hash, re-hashes raw password, compares
     * → Does NOT need to decrypt (one-way function)
     *
     * @return BCryptPasswordEncoder with default strength (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength=10 is the default. Increase to 12 for more security (but slower).
        return new BCryptPasswordEncoder();
    }
}
