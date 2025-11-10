package com.insurance.controller;

import com.insurance.dto.response.ApiResponse;
import com.insurance.dto.response.RiskAssessmentResponse;
import com.insurance.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Risk Assessment REST controller.
 * BASE URL: /api/v1/risk
 * ACCESS: ADMIN and AGENT only
 */
@RestController
@RequestMapping("/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Assessment", description = "APIs for customer risk profiling and scoring")
@SecurityRequirement(name = "Bearer Authentication")
public class RiskAssessmentController {

    private final RiskAssessmentService riskAssessmentService;

    /**
     * POST /api/v1/risk/assess/{customerId}
     * Triggers a fresh risk assessment calculation for a customer.
     * Use this after: creating a customer, updating profile, or processing a claim.
     */
    @PostMapping("/assess/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Calculate or recalculate risk assessment for a customer")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> assessRisk(@PathVariable Long customerId) {
        RiskAssessmentResponse response = riskAssessmentService.assessCustomerRisk(customerId);
        return ResponseEntity.ok(ApiResponse.success(
                "Risk assessment completed. Risk level: " + response.getRiskLevel(),
                response, 200
        ));
    }

    /**
     * GET /api/v1/risk/{customerId}
     * Retrieves the existing risk assessment for a customer.
     */
    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(summary = "Get existing risk assessment for a customer")
    public ResponseEntity<ApiResponse<RiskAssessmentResponse>> getRiskAssessment(@PathVariable Long customerId) {
        RiskAssessmentResponse response = riskAssessmentService.getRiskAssessment(customerId);
        return ResponseEntity.ok(ApiResponse.success("Risk assessment retrieved.", response, 200));
    }
}
