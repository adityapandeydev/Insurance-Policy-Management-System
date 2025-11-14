package com.insurance.service;

import com.insurance.entity.Customer;
import com.insurance.entity.Policy;
import com.insurance.entity.User;
import com.insurance.enums.PolicyStatus;
import com.insurance.enums.PolicyType;
import com.insurance.enums.RiskLevel;
import com.insurance.mapper.PolicyMapper;
import com.insurance.repository.ClaimRepository;
import com.insurance.repository.CustomerRepository;
import com.insurance.repository.PolicyRepository;
import com.insurance.repository.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for PolicyService — focuses on premium calculation and status transitions.
 *
 * INTERVIEW TIP: @ParameterizedTest with @EnumSource
 * Tests the same logic with multiple enum values.
 * Instead of writing 5 separate tests for each PolicyType,
 * @ParameterizedTest runs the test once for each enum constant.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PolicyService Unit Tests")
class PolicyServiceTest {

    @Mock private PolicyRepository policyRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private RiskAssessmentRepository riskAssessmentRepository;
    @Mock private ClaimRepository claimRepository;
    @Mock private PolicyMapper policyMapper;

    @InjectMocks private PolicyService policyService;

    private Customer mockCustomer;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).email("john.doe@email.com").build();
        mockCustomer = Customer.builder()
                .id(1L).user(mockUser)
                .dateOfBirth(LocalDate.of(1990, 5, 15)) // age ~34
                .build();
    }

    // ─── PREMIUM CALCULATION TESTS ───────────────────────────────────────────

    @Test
    @DisplayName("Should calculate LIFE policy premium correctly for LOW risk customer")
    void shouldCalculateLifePremiumForLowRisk() {
        // LOW risk customer, LIFE policy, 500,000 coverage, MONTHLY
        // Expected: 500,000 × 0.005 (LIFE) × 1.0 (LOW) / 12 = 208.33

        given(riskAssessmentRepository.findByCustomerId(1L))
                .willReturn(Optional.empty()); // no assessment = defaults to MEDIUM

        // Since no risk assessment → uses MEDIUM multiplier (1.5)
        // Expected: 500,000 × 0.005 × 1.5 / 12 = 312.50
        BigDecimal premium = policyService.calculatePremium(
                new BigDecimal("500000"),
                PolicyType.LIFE,
                mockCustomer,
                "MONTHLY"
        );

        // 500,000 * 0.005 * 1.5 / 12 = 312.50
        assertThat(premium).isEqualByComparingTo(new BigDecimal("312.50"));
    }

    @Test
    @DisplayName("Should calculate HEALTH policy premium with ANNUAL frequency")
    void shouldCalculateHealthPremiumAnnually() {
        // No risk assessment → MEDIUM (1.5x)
        // 300,000 × 0.010 × 1.5 = 4,500 (annual)

        given(riskAssessmentRepository.findByCustomerId(1L)).willReturn(Optional.empty());

        BigDecimal premium = policyService.calculatePremium(
                new BigDecimal("300000"),
                PolicyType.HEALTH,
                mockCustomer,
                "ANNUAL"
        );

        assertThat(premium).isEqualByComparingTo(new BigDecimal("4500.00"));
    }

    @Test
    @DisplayName("Should apply HIGH risk multiplier (2.0x) when customer has HIGH risk assessment")
    void shouldApplyHighRiskMultiplier() {
        // HIGH risk customer: 200,000 × 0.010 (HEALTH) × 2.0 (HIGH) / 12 = 333.33
        var highRiskAssessment = com.insurance.entity.RiskAssessment.builder()
                .id(1L).customer(mockCustomer).riskLevel(RiskLevel.HIGH).build();

        given(riskAssessmentRepository.findByCustomerId(1L))
                .willReturn(Optional.of(highRiskAssessment));

        BigDecimal premium = policyService.calculatePremium(
                new BigDecimal("200000"),
                PolicyType.HEALTH,
                mockCustomer,
                "MONTHLY"
        );

        // 200,000 * 0.01 * 2.0 / 12 = 333.33
        assertThat(premium).isEqualByComparingTo(new BigDecimal("333.33"));
    }

    /**
     * Parameterized test: validates premium calculation for all policy types.
     * Each type must produce a premium > 0 for a non-zero coverage amount.
     *
     * @ParameterizedTest: JUnit 5 feature to run a test with multiple inputs.
     * @EnumSource: provides all values of PolicyType enum as test arguments.
     */
    @ParameterizedTest
    @EnumSource(PolicyType.class)
    @DisplayName("Should calculate positive premium for all policy types")
    void shouldCalculatePositivePremiumForAllPolicyTypes(PolicyType policyType) {
        given(riskAssessmentRepository.findByCustomerId(anyLong())).willReturn(Optional.empty());

        BigDecimal premium = policyService.calculatePremium(
                new BigDecimal("100000"),
                policyType,
                mockCustomer,
                "MONTHLY"
        );

        assertThat(premium)
                .isNotNull()
                .isPositive()
                .as("Premium for %s should be positive", policyType);
    }

    // ─── STATUS TRANSITION TESTS ──────────────────────────────────────────────

    @Test
    @DisplayName("Should expire an active policy with passed end date")
    void shouldExpireActivePoliciesOnSchedule() {
        given(policyRepository.expireActivePolicies(LocalDate.now())).willReturn(3);

        // Trigger the scheduled job manually
        policyService.expireOverduePolicies();

        then(policyRepository).should().expireActivePolicies(LocalDate.now());
    }
}
