package com.insurance.config;

import com.insurance.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                     SPRING SECURITY CONFIGURATION                       ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  This is the central security configuration class.                      ║
 * ║  It defines: which endpoints are public, which require auth,            ║
 * ║  session management strategy, CSRF policy, CORS policy,                 ║
 * ║  and where our JWT filter sits in the filter chain.                     ║
 * ║                                                                          ║
 * ║  @EnableWebSecurity                                                     ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Enables Spring Security's web security support.                        ║
 * ║  Without it, Spring Boot's auto-config would apply default security     ║
 * ║  (all requests require login, form-based login enabled).                ║
 * ║                                                                          ║
 * ║  @EnableMethodSecurity                                                  ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Enables method-level security annotations:                             ║
 * ║  @PreAuthorize("hasRole('ADMIN')")  → checked before method executes   ║
 * ║  @PostAuthorize("...")             → checked after method executes      ║
 * ║  @Secured("ROLE_ADMIN")            → simpler role-based check          ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: SecurityFilterChain vs WebSecurityConfigurerAdapter     ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  In Spring Security 5.7+/Spring Boot 3.x, WebSecurityConfigurerAdapter ║
 * ║  is REMOVED. The new approach: define SecurityFilterChain as a @Bean.   ║
 * ║  This is more composable (multiple SecurityFilterChains can coexist).  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // enables @PreAuthorize, @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    // ─── PUBLIC ENDPOINTS ─────────────────────────────────────────────────
    // These URLs are accessible without a JWT token.
    private static final String[] PUBLIC_URLS = {
            "/auth/**",              // login, register
            "/swagger-ui/**",        // Swagger UI HTML and JS
            "/swagger-ui.html",
            "/v3/api-docs/**",       // OpenAPI JSON spec
            "/actuator/health",      // Health check endpoint
            "/actuator/info"
    };

    /**
     * SecurityFilterChain Bean — the heart of Spring Security configuration.
     *
     * The HttpSecurity builder configures:
     * 1. CORS: which origins/methods/headers are allowed
     * 2. CSRF: disabled for REST APIs (stateless, token-based)
     * 3. URL authorization rules: who can access which paths
     * 4. Session management: STATELESS (no server-side sessions)
     * 5. AuthenticationProvider: our custom provider with BCrypt
     * 6. JWT filter placement in the filter chain
     *
     * @param http The HttpSecurity builder
     * @return Configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            // ─── CORS Configuration ──────────────────────────────────────
            // CORS (Cross-Origin Resource Sharing): allows our frontend (React/Vite)
            // running on localhost:3000 to call our API on localhost:8080.
            // Without CORS config, browsers block cross-origin API calls.
            // INTERVIEW TIP: CORS is a browser security feature, NOT a server security feature.
            // API calls from Postman/curl bypass CORS restrictions (no browser enforcement).
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ─── CSRF Disabled ──────────────────────────────────────────
            // CSRF (Cross-Site Request Forgery) protection is NOT needed for JWT-based REST APIs.
            // Why? CSRF attacks exploit browser's automatic cookie sending.
            // Since we use JWT in Authorization header (not cookies), CSRF attacks don't apply.
            // Disabling CSRF is SAFE for stateless REST APIs with JWT authentication.
            .csrf(AbstractHttpConfigurer::disable)

            // ─── URL Authorization Rules ────────────────────────────────
            .authorizeHttpRequests(authz -> authz
                // Public endpoints — no authentication required
                .requestMatchers(PUBLIC_URLS).permitAll()

                // GET on customers: ADMIN and AGENT can list all; CUSTOMER can see their own
                // (Fine-grained customer access is enforced at service layer with @PreAuthorize)
                .requestMatchers(HttpMethod.GET, "/customers/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT", "ROLE_CUSTOMER")

                // Customer management (create, update, delete): ADMIN and AGENT only
                .requestMatchers(HttpMethod.POST, "/customers/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT")
                .requestMatchers(HttpMethod.PUT, "/customers/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT")
                .requestMatchers(HttpMethod.DELETE, "/customers/**").hasAnyAuthority("ROLE_ADMIN")

                // Policies: all authenticated users can view; ADMIN/AGENT can modify
                .requestMatchers(HttpMethod.GET, "/policies/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT", "ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/policies/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT", "ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/policies/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT", "ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.DELETE, "/policies/**").hasAuthority("ROLE_ADMIN")

                // Claims: customers can submit; agents/admins can review/approve/reject
                .requestMatchers(HttpMethod.GET, "/claims/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT", "ROLE_CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/claims/**")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT", "ROLE_CUSTOMER")
                .requestMatchers("/claims/*/approve", "/claims/*/reject", "/claims/*/review")
                    .hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT")

                // Risk assessment: ADMIN and AGENT only
                .requestMatchers("/risk/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT")

                // Dashboard: ADMIN and AGENT
                .requestMatchers("/dashboard/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_AGENT")

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // ─── Session Management ──────────────────────────────────────
            // STATELESS: Spring Security will NOT create or use HTTP sessions.
            // INTERVIEW TIP: In session-based auth, the server stores session in memory
            // or Redis. With JWT (stateless), EVERY request must carry the token.
            // Stateless is: horizontally scalable (no shared session state),
            // simpler (no session store needed), and suits microservices architecture.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ─── Authentication Provider ─────────────────────────────────
            // Registers our custom AuthenticationProvider (defined in ApplicationConfig).
            // This provider knows how to: load users from DB and verify BCrypt passwords.
            .authenticationProvider(authenticationProvider)

            // ─── JWT Filter Placement ─────────────────────────────────────
            // Add our JWT filter BEFORE the standard username/password filter.
            // Filter chain execution order:
            //   JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → ...
            //
            // If the JWT filter authenticates the request successfully, the
            // UsernamePasswordAuthenticationFilter is skipped (already authenticated).
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS Configuration Source.
     *
     * INTERVIEW TIP: CORS preflight requests
     * Before sending cross-origin POST/PUT/DELETE, browsers send an HTTP OPTIONS
     * request to check if the server allows the cross-origin request.
     * The server must respond with appropriate Access-Control-Allow-* headers.
     * Spring's CORS support handles these preflight responses automatically.
     *
     * @return CorsConfigurationSource with allowed origins, methods, headers
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins: React dev server + production domain
        // In production, replace with your actual domain
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",   // React/Vite dev server
                "http://localhost:5173",   // Vite default port
                "http://localhost:4200",   // Angular (if used)
                "https://your-production-domain.com"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed request headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",    // JWT token
                "Content-Type",     // JSON content type
                "Accept",           // Response format
                "X-Requested-With"  // AJAX indicator
        ));

        // Whether credentials (cookies, auth headers) can be included
        configuration.setAllowCredentials(true);

        // How long browsers should cache the preflight response (1 hour)
        configuration.setMaxAge(3600L);

        // Apply this CORS config to all API paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
