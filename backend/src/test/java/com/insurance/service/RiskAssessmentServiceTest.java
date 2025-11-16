package com.insurance.service;

import com.insurance.entity.Customer;
import com.insurance.entity.Policy;
import com.insurance.entity.RiskAssessment;
import com.insurance.entity.User;
import com.insurance.enums.PolicyStatus;
import com.insurance.enums.RiskLevel;
import com.insurance.repository.ClaimRepository;
import com.insurance.repository.CustomerRepository;
import com.insurance.repository.PolicyRepository;
import com.insurance.repository.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for RiskAssessmentService — validates the scoring algorithm.
 *
 * INTERVIEW TIP: ArgumentCaptor in Mockito
 * Used when you want to inspect the actual argument passed to a mock method.
 * Example: verify that save() was called with a RiskAssessment that has
 * the expected riskLevel = HIGH. Without captor, you'd have to use
 * argThat() which is less readable.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAssessmentService Unit Tests")
class RiskAssessmentServiceTest {

    @Mock private RiskAssessmentRepository riskAssessmentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private ClaimRepository claimRepository;

    @InjectMocks private RiskAssessmentService riskAssessmentService;

    private Customer youngCustomer;     // age ~29, low risk
    private Customer elderlyCustomer;   // age ~59, high risk
    private Policy activePolicyLow;

    @BeforeEach
    void setUp() {
        User youngUser = User.builder().id(1L).email("young@email.com").build();
        youngCustomer = Customer.builder()
                .id(1L).user(youngUser)
                .dateOfBirth(LocalDate.of(1995, 6, 1))  // ~29 years old
                .build();

        User elderlyUser = User.builder().id(2L).email("elderly@email.com").build();
        elderlyCustomer = Customer.builder()
                .id(2L).user(elderlyUser)
                .dateOfBirth(LocalDate.of(1965, 3, 15))  // ~59 years old
                .build();

        activePolicyLow = Policy.builder()
                .id(1L)
                .status(PolicyStatus.ACTIVE)
                .startDate(LocalDate.now().minusYears(1))
                .endDate(LocalDate.now().plusYears(1))
                .coverageAmount(new BigDecimal("50000")) // low coverage → score = 2
                .build();
    }

    @Test
    @DisplayName("Young customer with low coverage and no claims should be LOW risk")
    void shouldAssignLowRiskToCleanYoungCustomer() {
        // ARRANGE
        given(customerRepository.findById(1L)).willReturn(Optional.of(youngCustomer));
        // Low coverage policy (< 100k → score 2)
        given(policyRepository.findByCustomerId(1L)).willReturn(List.of(activePolicyLow));
        given(claimRepository.countApprovedClaimsByCustomer(1L)).willReturn(0L); // clean history
        given(riskAssessmentRepository.findByCustomerId(1L)).willReturn(Optional.empty());

        // Capture the saved RiskAssessment to verify its content
        ArgumentCaptor<RiskAssessment> captor = ArgumentCaptor.forClass(RiskAssessment.class);
        given(riskAssessmentRepository.save(captor.capture())).willAnswer(i -> i.getArgument(0));

        // ACT
        var response = riskAssessmentService.assessCustomerRisk(1L);

        // ASSERT: response
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(response.getTotalRiskScore()).isLessThan(new BigDecimal("4.0"));

        // ASSERT: saved entity
        RiskAssessment saved = captor.getValue();
        assertThat(saved.getRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(saved.getClaimHistoryScore()).isEqualTo(1); // 0 claims → 1 pt
    }

    @Test
    @DisplayName("Elderly customer with high coverage and claim history should be HIGH risk")
    void shouldAssignHighRiskToElderlyCustomerWithClaims() {
        // ARRANGE
        given(customerRepository.findById(2L)).willReturn(Optional.of(elderlyCustomer));

        // High coverage policy (> 1M → score 10)
        Policy highCoveragePolicy = Policy.builder()
                .id(2L).status(PolicyStatus.ACTIVE)
                .startDate(LocalDate.now().minusYears(2))
                .endDate(LocalDate.now().plusYears(3))
                .coverageAmount(new BigDecimal("2000000")) // > 1M → score = 10
                .build();
        given(policyRepository.findByCustomerId(2L)).willReturn(List.of(highCoveragePolicy));
        given(claimRepository.countApprovedClaimsByCustomer(2L)).willReturn(4L); // 3-5 → score = 7
        given(riskAssessmentRepository.findByCustomerId(2L)).willReturn(Optional.empty());

        ArgumentCaptor<RiskAssessment> captor = ArgumentCaptor.forClass(RiskAssessment.class);
        given(riskAssessmentRepository.save(captor.capture())).willAnswer(i -> i.getArgument(0));

        // ACT
        // Age 59 → score 9 (>60 bracket), Coverage >1M → score 10, 4 claims → score 7
        // Total = (9 × 0.3) + (10 × 0.4) + (7 × 0.3) = 2.7 + 4.0 + 2.1 = 8.8 → HIGH
        var response = riskAssessmentService.assessCustomerRisk(2L);

        // ASSERT
        assertThat(response.getRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(response.getTotalRiskScore()).isGreaterThan(new BigDecimal("7.0"));

        RiskAssessment saved = captor.getValue();
        assertThat(saved.getAgeScore()).isEqualTo(9);
        assertThat(saved.getCoverageScore()).isEqualTo(10);
        assertThat(saved.getClaimHistoryScore()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should UPDATE existing risk assessment (not create new one)")
    void shouldUpdateExistingRiskAssessment() {
        // ARRANGE: customer already has a risk assessment
        RiskAssessment existingAssessment = RiskAssessment.builder()
                .id(5L).customer(youngCustomer)
                .riskLevel(RiskLevel.MEDIUM) // was MEDIUM before
                .build();

        given(customerRepository.findById(1L)).willReturn(Optional.of(youngCustomer));
        given(policyRepository.findByCustomerId(1L)).willReturn(List.of(activePolicyLow));
        given(claimRepository.countApprovedClaimsByCustomer(1L)).willReturn(0L);
        given(riskAssessmentRepository.findByCustomerId(1L)).willReturn(Optional.of(existingAssessment));
        given(riskAssessmentRepository.save(any())).willAnswer(i -> i.getArgument(0));

        // ACT
        riskAssessmentService.assessCustomerRisk(1L);

        // ASSERT: save() called once (update, not create)
        then(riskAssessmentRepository).should(times(1)).save(any());
    }
}
