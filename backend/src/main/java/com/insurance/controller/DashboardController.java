package com.insurance.controller;

import com.insurance.dto.response.ApiResponse;
import com.insurance.dto.response.DashboardResponse;
import com.insurance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard REST controller — ADMIN only.
 * BASE URL: /api/v1/dashboard
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Admin dashboard analytics and system metrics")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * GET /api/v1/dashboard
     * Returns aggregated system metrics for the admin dashboard.
     * ADMIN only — contains sensitive system-wide statistics.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_AGENT')")
    @Operation(
        summary = "Get system dashboard metrics",
        description = "Returns aggregated counts: customers, policies (by status), claims (by status), risk levels, and user counts. ADMIN only."
    )
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse response = dashboardService.getDashboardData();
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully.", response, 200));
    }
}
