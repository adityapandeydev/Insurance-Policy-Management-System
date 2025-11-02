package com.insurance.controller;

import com.insurance.dto.request.PolicyRequest;
import com.insurance.dto.response.ApiResponse;
import com.insurance.dto.response.PolicyResponse;
import com.insurance.enums.PolicyStatus;
import com.insurance.service.PolicyService;
import io.swagger.v3.oas.annotations.Operation;
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
 * Policy management REST controller.
 * BASE URL: /api/v1/policies
 *
 * ENDPOINTS:
 * POST   /policies                  → Create policy (ADMIN/AGENT)
 * GET    /policies                  → List all policies (paginated)
 * GET    /policies/{id}             → Get policy by ID
 * GET    /policies/customer/{cid}   → Get policies for a customer
 * PUT    /policies/{id}             → Update policy
 * PATCH  /policies/{id}/status      → Change policy status
 * DELETE /policies/{id}             → Delete policy (ADMIN only)
 */
@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Policy Management", description = "APIs for insurance policy lifecycle management")
@SecurityRequirement(name = "Bearer Authentication")
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Create a new insurance policy")
    public ResponseEntity<ApiResponse<PolicyResponse>> createPolicy(
            @Valid @RequestBody PolicyRequest request
    ) {
        PolicyResponse response = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Policy created successfully.", response, 201));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT', 'ROLE_CUSTOMER')")
    @Operation(summary = "List all policies with pagination and search")
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> getAllPolicies(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<PolicyResponse> policies = policyService.getAllPolicies(search, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Policies retrieved successfully.", policies, 200));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get policy details by ID")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Policy retrieved.", policyService.getPolicyById(id), 200));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT', 'ROLE_CUSTOMER')")
    @Operation(summary = "Get all policies for a customer")
    public ResponseEntity<ApiResponse<Page<PolicyResponse>>> getPoliciesByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<PolicyResponse> policies = policyService.getPoliciesByCustomer(
                customerId, PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(ApiResponse.success("Customer policies retrieved.", policies, 200));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Update policy details")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Policy updated.", policyService.updatePolicy(id, request), 200));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Change policy status (ACTIVATE, CANCEL, etc.)")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicyStatus(
            @PathVariable Long id,
            @RequestParam PolicyStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Policy status updated to " + status + ".",
                policyService.updatePolicyStatus(id, status), 200
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Delete a policy (only PENDING or CANCELLED)")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.ok(ApiResponse.success("Policy deleted successfully.", 200));
    }
}
