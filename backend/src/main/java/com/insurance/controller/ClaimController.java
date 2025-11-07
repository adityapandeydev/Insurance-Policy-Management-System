package com.insurance.controller;

import com.insurance.dto.request.ClaimRequest;
import com.insurance.dto.request.ClaimReviewRequest;
import com.insurance.dto.response.ApiResponse;
import com.insurance.dto.response.ClaimResponse;
import com.insurance.enums.ClaimStatus;
import com.insurance.service.ClaimService;
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
 * Claims management REST controller.
 * BASE URL: /api/v1/claims
 *
 * ENDPOINTS:
 * POST  /claims                    → Submit a claim (any authenticated user with customer profile)
 * GET   /claims                    → List claims (filtered by role/ownership)
 * GET   /claims/{id}               → Get claim details
 * GET   /claims/policy/{policyId}  → Get claims for a policy
 * POST  /claims/{id}/review        → Start review (ADMIN/AGENT)
 * POST  /claims/{id}/approve       → Approve claim (ADMIN/AGENT)
 * POST  /claims/{id}/reject        → Reject claim (ADMIN/AGENT)
 * POST  /claims/{id}/withdraw      → Withdraw claim (CUSTOMER - own claims)
 */
@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Claims Management", description = "APIs for insurance claims submission, tracking, and review")
@SecurityRequirement(name = "Bearer Authentication")
public class ClaimController {

    private final ClaimService claimService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a new insurance claim")
    public ResponseEntity<ApiResponse<ClaimResponse>> submitClaim(
            @Valid @RequestBody ClaimRequest request
    ) {
        ClaimResponse response = claimService.submitClaim(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Claim submitted successfully. Claim number: " + response.getClaimNumber(), response, 201));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List claims (filtered by role: CUSTOMER sees own, ADMIN/AGENT sees all)")
    public ResponseEntity<ApiResponse<Page<ClaimResponse>>> getAllClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Page<ClaimResponse> claims = claimService.getAllClaims(status, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success("Claims retrieved successfully.", claims, 200));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get claim details by ID")
    public ResponseEntity<ApiResponse<ClaimResponse>> getClaimById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Claim retrieved.", claimService.getClaimById(id), 200));
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Get all claims for a specific policy")
    public ResponseEntity<ApiResponse<Page<ClaimResponse>>> getClaimsByPolicy(
            @PathVariable Long policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ClaimResponse> claims = claimService.getClaimsByPolicy(
                policyId, PageRequest.of(page, size, Sort.by("submittedAt").descending())
        );
        return ResponseEntity.ok(ApiResponse.success("Policy claims retrieved.", claims, 200));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Start reviewing a PENDING claim")
    public ResponseEntity<ApiResponse<ClaimResponse>> startReview(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Claim moved to UNDER_REVIEW.", claimService.startClaimReview(id), 200));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Approve a claim (irreversible action)")
    public ResponseEntity<ApiResponse<ClaimResponse>> approveClaim(
            @PathVariable Long id,
            @Valid @RequestBody ClaimReviewRequest review
    ) {
        return ResponseEntity.ok(ApiResponse.success("Claim APPROVED successfully.", claimService.approveClaim(id, review), 200));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Reject a claim with mandatory review notes")
    public ResponseEntity<ApiResponse<ClaimResponse>> rejectClaim(
            @PathVariable Long id,
            @Valid @RequestBody ClaimReviewRequest review
    ) {
        return ResponseEntity.ok(ApiResponse.success("Claim REJECTED.", claimService.rejectClaim(id, review), 200));
    }

    @PostMapping("/{id}/withdraw")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Withdraw a PENDING claim (customer only, own claims)")
    public ResponseEntity<ApiResponse<ClaimResponse>> withdrawClaim(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Claim WITHDRAWN successfully.", claimService.withdrawClaim(id), 200));
    }
}
