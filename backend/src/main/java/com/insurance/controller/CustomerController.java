package com.insurance.controller;

import com.insurance.dto.request.CustomerRequest;
import com.insurance.dto.response.ApiResponse;
import com.insurance.dto.response.CustomerResponse;
import com.insurance.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                      CUSTOMER CONTROLLER                                ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  BASE URL: /api/v1/customers                                            ║
 * ║                                                                          ║
 * ║  ENDPOINTS:                                                              ║
 * ║  POST   /customers/{userId}/profile  → Create customer profile          ║
 * ║  GET    /customers/me                → My profile (customer role)       ║
 * ║  GET    /customers                   → List all (admin/agent only)      ║
 * ║  GET    /customers/{id}              → Get by ID                        ║
 * ║  PUT    /customers/{id}              → Update customer                  ║
 * ║  DELETE /customers/{id}              → Delete customer (admin only)     ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "APIs for managing customer profiles")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * POST /api/v1/customers/{userId}/profile
     * Creates a customer profile for an existing user.
     * ADMIN or AGENT only.
     */
    @PostMapping("/{userId}/profile")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Create customer profile for a user")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomerProfile(
            @PathVariable Long userId,
            @Valid @RequestBody CustomerRequest request
    ) {
        CustomerResponse response = customerService.createCustomerProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer profile created successfully.", response, 201));
    }

    /**
     * GET /api/v1/customers/me
     * Returns the currently authenticated customer's own profile.
     * CUSTOMER role endpoint — must come before /{id} mapping.
     *
     * INTERVIEW TIP: Path ordering in Spring MVC matters.
     * /customers/me must be declared BEFORE /customers/{id} otherwise
     * Spring would try to parse "me" as a Long and throw NumberFormatException.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('ROLE_CUSTOMER', 'ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Get current user's customer profile")
    public ResponseEntity<ApiResponse<CustomerResponse>> getMyProfile() {
        CustomerResponse response = customerService.getCurrentCustomerProfile();
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully.", response, 200));
    }

    /**
     * GET /api/v1/customers?search=&page=0&size=10&sort=createdAt,desc
     * Paginated list of all customers with optional search.
     * ADMIN and AGENT only.
     *
     * INTERVIEW TIP: @RequestParam with defaults allows:
     * - /customers               → page 0, size 10, sorted by createdAt desc
     * - /customers?search=John  → filtered results
     * - /customers?page=2&size=5 → third page with 5 records per page
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "List all customers with pagination and search")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getAllCustomers(
            @RequestParam(required = false) @Parameter(description = "Search by name, email, or phone") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<CustomerResponse> customers = customerService.getAllCustomers(
                search, PageRequest.of(page, size, sort)
        );
        return ResponseEntity.ok(ApiResponse.success("Customers retrieved successfully.", customers, 200));
    }

    /**
     * GET /api/v1/customers/{id}
     * Retrieves a customer by ID.
     * CUSTOMER: can only access their own. ADMIN/AGENT: access any.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable Long id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success("Customer retrieved successfully.", response, 200));
    }

    /**
     * PUT /api/v1/customers/{id}
     * Updates a customer profile.
     * CUSTOMER: can only update own profile. ADMIN/AGENT: can update any.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT', 'ROLE_CUSTOMER')")
    @Operation(summary = "Update customer profile")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest request
    ) {
        CustomerResponse response = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully.", response, 200));
    }

    /**
     * DELETE /api/v1/customers/{id}
     * Soft-deletes a customer (disables their account).
     * ADMIN only.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete (soft-disable) a customer account")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer account disabled successfully.", 200));
    }
}
