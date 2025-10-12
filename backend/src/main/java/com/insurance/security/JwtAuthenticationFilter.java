package com.insurance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                    JWT AUTHENTICATION FILTER                            ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  This filter intercepts EVERY HTTP request and validates the JWT token. ║
 * ║  It runs BEFORE Spring Security's normal authentication mechanisms.     ║
 * ║                                                                          ║
 * ║  EXTENDS OncePerRequestFilter                                           ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Guarantees this filter runs EXACTLY ONCE per HTTP request, even if     ║
 * ║  the request is forwarded internally (e.g., error forwarding).          ║
 * ║  Without this, some filters could run twice per request.                ║
 * ║                                                                          ║
 * ║  FILTER EXECUTION FLOW:                                                 ║
 * ║  HTTP Request                                                            ║
 * ║      │                                                                   ║
 * ║      ▼                                                                   ║
 * ║  [JwtAuthenticationFilter]                                              ║
 * ║      │ 1. Extract "Authorization" header                                ║
 * ║      │ 2. If no "Bearer " token → skip (pass to next filter)           ║
 * ║      │ 3. Extract username from token                                   ║
 * ║      │ 4. Load UserDetails from DB                                      ║
 * ║      │ 5. Validate token                                                ║
 * ║      │ 6. Set Authentication in SecurityContextHolder                  ║
 * ║      ▼                                                                   ║
 * ║  [Next Filter in Chain] → [Controller]                                 ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: SecurityContextHolder                                   ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  It uses ThreadLocal to store the Authentication object for the         ║
 * ║  current request thread. After the request completes, ThreadLocal is    ║
 * ║  cleared to prevent memory leaks (done automatically by Spring).       ║
 * ║  This is why @PreAuthorize in any @Service can access the current user. ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Component
@RequiredArgsConstructor  // Lombok: generates constructor for all final fields (DI without @Autowired)
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Core filter logic. Extracts, validates JWT, and populates SecurityContext.
     *
     * @param request     The incoming HTTP request
     * @param response    The HTTP response (to write error messages if needed)
     * @param filterChain The chain of remaining filters to execute after this one
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // ─── STEP 1: Extract Authorization Header ─────────────────────────
        // JWT tokens are sent in the "Authorization" header with "Bearer " prefix:
        // Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSJ9...
        final String authHeader = request.getHeader("Authorization");

        // ─── STEP 2: Guard Clause — skip if no Bearer token ───────────────
        // If the header is missing or doesn't start with "Bearer ", this is either:
        // (a) A public endpoint (login, register) — allow through, Spring Security
        //     will handle it based on SecurityConfig permit rules.
        // (b) A request with no auth — Spring Security will return 401 Unauthorized.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No JWT token found in request to: {}", request.getRequestURI());
            filterChain.doFilter(request, response);  // pass to next filter
            return;
        }

        // ─── STEP 3: Extract JWT Token ─────────────────────────────────────
        // Remove the "Bearer " prefix (7 characters) to get the raw token.
        final String jwt = authHeader.substring(7);

        // ─── STEP 4: Extract Username from Token ───────────────────────────
        // This also validates the token's signature and format.
        // If the token is malformed, JwtService throws an exception.
        final String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Invalid token (malformed, bad signature, etc.)
            log.warn("JWT token validation error: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        // ─── STEP 5: Validate and Authenticate ─────────────────────────────
        // SecurityContextHolder.getContext().getAuthentication() == null means:
        // this request hasn't been authenticated yet in this filter chain run.
        // We only proceed if: username is present AND user isn't already authenticated.
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load full UserDetails from database by email (username)
            // This triggers a DB query: SELECT * FROM users WHERE email = ?
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Validate: does the token belong to this user and is it not expired?
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // ─── STEP 6: Create Authentication Token ──────────────────
                // UsernamePasswordAuthenticationToken is Spring Security's
                // standard Authentication implementation for username/password-based auth.
                //
                // Parameters:
                //   principal:   The UserDetails (who is authenticated)
                //   credentials: null (we use JWT, not passwords post-login)
                //   authorities: The user's roles/permissions
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Attach request details (IP address, session info) to the auth token
                // This is used by Spring Security for logging and auditing purposes.
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // ─── STEP 7: Set Authentication in SecurityContext ─────────
                // This is the critical step. By setting the authentication here,
                // we're telling Spring Security: "this request is authenticated".
                // All subsequent @PreAuthorize checks will use this authentication.
                //
                // ThreadLocal storage: each request thread has its own SecurityContext.
                // The security context is cleared automatically after the request completes.
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authentication successful for user: {}, role: {}",
                        userEmail, userDetails.getAuthorities());
            }
        }

        // ─── STEP 8: Continue the filter chain ────────────────────────────
        // Whether we authenticated successfully or not, we always continue.
        // If authentication failed, the request will be rejected later by
        // Spring Security's authorization layer (403 Forbidden).
        filterChain.doFilter(request, response);
    }
}
