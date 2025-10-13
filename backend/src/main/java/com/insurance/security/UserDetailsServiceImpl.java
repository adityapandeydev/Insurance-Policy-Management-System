package com.insurance.security;

import com.insurance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                   USER DETAILS SERVICE IMPL                             ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Implements Spring Security's UserDetailsService interface.             ║
 * ║  This is the bridge between Spring Security and our database.           ║
 * ║                                                                          ║
 * ║  SPRING SECURITY AUTHENTICATION FLOW:                                   ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  1. User sends POST /auth/login with {email, password}                  ║
 * ║  2. AuthService calls authenticationManager.authenticate(...)           ║
 * ║  3. AuthenticationManager asks DaoAuthenticationProvider               ║
 * ║  4. DaoAuthenticationProvider calls loadUserByUsername(email)          ║
 * ║  5. We query DB: SELECT * FROM users WHERE email = ?                   ║
 * ║  6. DaoAuthenticationProvider compares stored hash with raw password   ║
 * ║     using BCryptPasswordEncoder.matches(rawPassword, storedHash)       ║
 * ║  7. If match: returns authenticated Authentication object              ║
 * ║  8. If no match: throws BadCredentialsException                        ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: Why separate UserDetailsService from AuthService?       ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  UserDetailsService is a Spring Security contract (interface).         ║
 * ║  Implementing it here means Spring Security can use our DB to          ║
 * ║  look up users during authentication — decoupling the framework        ║
 * ║  from our business logic.                                               ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their username (email in our system).
     *
     * Called by Spring Security's DaoAuthenticationProvider during login,
     * AND by our JwtAuthenticationFilter on every JWT-secured request.
     *
     * @Transactional: Opens a transaction for this method.
     * Ensures the User entity (and its lazy-loaded relationships) can be
     * accessed within this method's scope without a LazyInitializationException.
     *
     * @param username The email address (used as username)
     * @return UserDetails (our User entity implements UserDetails directly)
     * @throws UsernameNotFoundException if no user with this email exists
     */
    @Override
    @Transactional(readOnly = true)  // readOnly=true: optimization hint to DB for SELECT-only transactions
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", username);

        // JPA query: SELECT * FROM users WHERE email = ?
        // UserRepository extends JpaRepository, so findByEmail() is a derived query method.
        return userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    log.warn("User not found with email: {}", username);
                    return new UsernameNotFoundException(
                            "User not found with email: " + username
                    );
                });
    }
}
