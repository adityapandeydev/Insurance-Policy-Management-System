package com.insurance.controller;

import com.insurance.dto.request.LoginRequest;
import com.insurance.dto.request.RegisterRequest;
import com.insurance.dto.response.ApiResponse;
import com.insurance.dto.response.AuthResponse;
import com.insurance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                       AUTHENTICATION CONTROLLER                         ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Handles public authentication endpoints (register and login).          ║
 * ║  These endpoints are explicitly permitted in SecurityConfig.            ║
 * ║                                                                          ║
 * ║  BASE URL: /api/v1/auth (context-path + controller mapping)            ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: @RestController vs @Controller                         ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  @Controller: returns view names (for server-side rendering with        ║
 * ║    Thymeleaf/JSP). Methods need @ResponseBody for JSON output.          ║
 * ║  @RestController = @Controller + @ResponseBody applied to ALL methods. ║
 * ║  Every method returns the object serialized directly as JSON.           ║
 * ║                                                                          ║
 * ║  @Valid: Triggers Jakarta Bean Validation on the @RequestBody.          ║
 * ║  If validation fails → MethodArgumentNotValidException → 400 response  ║
 * ║  handled by GlobalExceptionHandler.                                     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User registration and login endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/register
     *
     * Registers a new user (with CUSTOMER role) and returns a JWT token.
     * This endpoint is publicly accessible (no token required).
     *
     * @param request Registration payload (firstName, lastName, email, password)
     * @return 201 Created with AuthResponse (JWT token + user info)
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new customer account",
        description = "Creates a new user account with CUSTOMER role. Returns a JWT token for immediate use."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Validation failed or email already exists"),
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        log.info("Registration request received for email: {}", request.getEmail());
        AuthResponse authResponse = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Registration successful! Welcome to Insurance Portal.",
                        authResponse,
                        HttpStatus.CREATED.value()
                ));
    }

    /**
     * POST /api/v1/auth/login
     *
     * Authenticates a user and returns a JWT token.
     * This endpoint is publicly accessible (no token required).
     *
     * @param request Login credentials (email, password)
     * @return 200 OK with AuthResponse (JWT token + user info)
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login with email and password",
        description = "Authenticates the user and returns a JWT Bearer token. " +
                      "Include this token in the Authorization header for secured endpoints."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Invalid email or password"),
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        log.info("Login request received for email: {}", request.getEmail());
        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful! Welcome back, " + authResponse.getFirstName() + ".",
                        authResponse,
                        HttpStatus.OK.value()
                )
        );
    }
}
