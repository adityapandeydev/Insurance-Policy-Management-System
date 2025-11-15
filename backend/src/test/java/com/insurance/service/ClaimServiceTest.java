package com.insurance.service;

import com.insurance.dto.request.ClaimRequest;
import com.insurance.dto.request.ClaimReviewRequest;
import com.insurance.dto.response.ClaimResponse;
import com.insurance.entity.Claim;
import com.insurance.entity.Customer;
import com.insurance.entity.Policy;
import com.insurance.entity.User;
import com.insurance.enums.ClaimStatus;
import com.insurance.enums.PolicyStatus;
import com.insurance.enums.Role;
import com.insurance.exception.BusinessRuleException;
import com.insurance.exception.ResourceNotFoundException;
import com.insurance.mapper.ClaimMapper;
import com.insurance.repository.ClaimRepository;
import com.insurance.repository.CustomerRepository;
import com.insurance.repository.PolicyRepository;
import com.insurance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for ClaimService business rule enforcement.
 *
 * INTERVIEW TIP: Testing business rules is critical.
 * Each test validates ONE specific rule or scenario.
 * Names follow: should_[expected]_when_[condition] pattern.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClaimService Unit Tests")
class ClaimServiceTest {

    @Mock private ClaimRepository claimRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClaimMapper claimMapper;

    @InjectMocks private ClaimService claimService;

    private User mockUser;
    private Customer mockCustomer;
    private Policy mockActivePolicy;
    private ClaimRequest validClaimRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L).email("john.doe@email.com")
                .role(Role.ROLE_CUSTOMER).enabled(true).build();

        mockCustomer = Customer.builder()
                .id(1L).user(mockUser).build();

        mockActivePolicy = Policy.builder()
                .id(1L)
                .policyNumber("POL-20240101-00001")
                .policyName("Test Policy")
                .status(PolicyStatus.ACTIVE)
                .coverageAmount(new BigDecimal("500000"))
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now().plusYears(1))
                .customer(mockCustomer)
                .build();

        validClaimRequest = ClaimRequest.builder()
                .policyId(1L)
                .description("Hospital admission for surgery requiring extensive treatment and care")
                .claimAmount(new BigDecimal("50000"))
                .incidentDate(LocalDate.now().minusDays(5))
                .build();

        // Set up Spring Security context with CUSTOMER role
        setSecurityContext("john.doe@email.com", "ROLE_CUSTOMER");
    }

    // ────────────────────────────────────────────────────────────────────────
    // BUSINESS RULE TESTS
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Business Rule: Policy Status")
    class PolicyStatusRuleTests {

        @Test
        @DisplayName("Should throw exception when claiming against EXPIRED policy")
        void shouldThrowWhenPolicyIsExpired() {
            // ARRANGE: expired policy
            mockActivePolicy.setStatus(PolicyStatus.EXPIRED);
            given(policyRepository.findById(1L)).willReturn(Optional.of(mockActivePolicy));

            // ACT + ASSERT
            assertThatThrownBy(() -> claimService.submitClaim(validClaimRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("Should throw exception when claiming against CANCELLED policy")
        void shouldThrowWhenPolicyIsCancelled() {
            mockActivePolicy.setStatus(PolicyStatus.CANCELLED);
            given(policyRepository.findById(1L)).willReturn(Optional.of(mockActivePolicy));

            assertThatThrownBy(() -> claimService.submitClaim(validClaimRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("Should throw exception when claiming against PENDING policy")
        void shouldThrowWhenPolicyIsPending() {
            mockActivePolicy.setStatus(PolicyStatus.PENDING);
            given(policyRepository.findById(1L)).willReturn(Optional.of(mockActivePolicy));

            assertThatThrownBy(() -> claimService.submitClaim(validClaimRequest))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("Business Rule: Claim Amount")
    class ClaimAmountRuleTests {

        @Test
        @DisplayName("Should throw exception when claim amount exceeds policy coverage")
        void shouldThrowWhenClaimExceedsCoverage() {
            // ARRANGE: claim amount > coverage amount
            validClaimRequest.setClaimAmount(new BigDecimal("999999")); // > 500000 coverage
            given(policyRepository.findById(1L)).willReturn(Optional.of(mockActivePolicy));
            given(customerRepository.findByUserEmail(anyString())).willReturn(Optional.of(mockCustomer));

            // ACT + ASSERT
            assertThatThrownBy(() -> claimService.submitClaim(validClaimRequest))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("coverage amount");
        }

        @Test
        @DisplayName("Should allow claim when amount equals policy coverage (boundary test)")
        void shouldAllowClaimEqualToCoverage() {
            // ARRANGE: claim amount = coverage amount exactly
            validClaimRequest.setClaimAmount(new BigDecimal("500000"));
            given(policyRepository.findById(1L)).willReturn(Optional.of(mockActivePolicy));
            given(customerRepository.findByUserEmail(anyString())).willReturn(Optional.of(mockCustomer));

            Claim savedClaim = buildMockClaim();
            ClaimResponse claimResponse = new ClaimResponse();
            claimResponse.setClaimNumber("CLM-20240101-00001");

            given(claimRepository.existsByClaimNumber(anyString())).willReturn(false);
            given(claimMapper.toEntity(any())).willReturn(savedClaim);
            given(claimRepository.save(any())).willReturn(savedClaim);
            given(claimMapper.toResponse(any())).willReturn(claimResponse);

            // ACT: should NOT throw exception
            assertThatCode(() -> claimService.submitClaim(validClaimRequest))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Business Rule: Claim Status Transitions")
    class ClaimStatusTransitionTests {

        @Test
        @DisplayName("Should throw when trying to approve an already APPROVED claim (terminal state)")
        void shouldThrowWhenApprovingAlreadyApprovedClaim() {
            Claim approvedClaim = buildMockClaim();
            approvedClaim.setStatus(ClaimStatus.APPROVED);

            given(claimRepository.findById(1L)).willReturn(Optional.of(approvedClaim));

            ClaimReviewRequest review = ClaimReviewRequest.builder()
                    .reviewNotes("Already approved, trying again").build();

            assertThatThrownBy(() -> claimService.approveClaim(1L, review))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("terminal state");
        }

        @Test
        @DisplayName("Should throw when trying to withdraw an APPROVED claim")
        void shouldThrowWhenWithdrawingApprovedClaim() {
            Claim approvedClaim = buildMockClaim();
            approvedClaim.setStatus(ClaimStatus.APPROVED);
            // Customer owns this claim
            given(claimRepository.findById(1L)).willReturn(Optional.of(approvedClaim));

            assertThatThrownBy(() -> claimService.withdrawClaim(1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("Should throw when reviewing a non-existent claim")
        void shouldThrowWhenClaimNotFound() {
            given(claimRepository.findById(999L)).willReturn(Optional.empty());

            ClaimReviewRequest review = new ClaimReviewRequest("Some review notes here");
            assertThatThrownBy(() -> claimService.approveClaim(999L, review))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Claim");
        }
    }

    // ─── HELPER METHODS ──────────────────────────────────────────────────────

    private Claim buildMockClaim() {
        return Claim.builder()
                .id(1L)
                .claimNumber("CLM-20240101-00001")
                .description("Test claim description with enough characters")
                .claimAmount(new BigDecimal("50000"))
                .status(ClaimStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .incidentDate(LocalDate.now().minusDays(5))
                .policy(mockActivePolicy)
                .customer(mockCustomer)
                .build();
    }

    /**
     * Sets up the Spring Security context for tests.
     * Simulates an authenticated user without going through the full JWT flow.
     *
     * INTERVIEW TIP: In unit tests, we manually set the SecurityContext.
     * In @WebMvcTest controller tests, we use @WithMockUser or
     * SecurityMockMvcRequestPostProcessors.jwt() for more realistic simulation.
     */
    private void setSecurityContext(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
