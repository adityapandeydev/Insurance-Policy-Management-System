package com.insurance.service;

import com.insurance.dto.request.PolicyRequest;
import com.insurance.dto.response.PolicyResponse;
import com.insurance.entity.Customer;
import com.insurance.entity.Policy;
import com.insurance.entity.RiskAssessment;
import com.insurance.enums.PolicyStatus;
import com.insurance.enums.PolicyType;
import com.insurance.enums.RiskLevel;
import com.insurance.exception.BusinessRuleException;
import com.insurance.exception.ResourceNotFoundException;
import com.insurance.mapper.PolicyMapper;
import com.insurance.repository.ClaimRepository;
import com.insurance.repository.CustomerRepository;
import com.insurance.repository.PolicyRepository;
import com.insurance.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                        POLICY SERVICE                                   ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  PREMIUM CALCULATION ALGORITHM:                                         ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Step 1: Base Rate by Policy Type                                       ║
 * ║    LIFE     → 0.5% of coverage per annum                               ║
 * ║    HEALTH   → 1.0% of coverage per annum                               ║
 * ║    VEHICLE  → 2.0% of coverage per annum                               ║
 * ║    PROPERTY → 0.8% of coverage per annum                               ║
 * ║    TRAVEL   → 3.0% of coverage per annum                               ║
 * ║                                                                          ║
 * ║  Step 2: Risk Multiplier                                                ║
 * ║    LOW    → 1.0x (no surcharge)                                        ║
 * ║    MEDIUM → 1.5x (50% surcharge)                                       ║
 * ║    HIGH   → 2.0x (100% surcharge)                                      ║
 * ║                                                                          ║
 * ║  Step 3: Convert to monthly if needed                                   ║
 * ║    annualPremium = coverageAmount × baseRate × riskMultiplier          ║
 * ║    monthlyPremium = annualPremium / 12                                  ║
 * ║                                                                          ║
 * ║  Example: John (MEDIUM risk) wants HEALTH policy, coverage = 500,000   ║
 * ║    annualPremium = 500,000 × 0.01 × 1.5 = 7,500/year                  ║
 * ║    monthlyPremium = 7,500 / 12 = 625/month                             ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ClaimRepository claimRepository;
    private final PolicyMapper policyMapper;

    // Base annual rates per policy type (as decimal fractions of coverage amount)
    private static final BigDecimal LIFE_BASE_RATE = new BigDecimal("0.005");     // 0.5%
    private static final BigDecimal HEALTH_BASE_RATE = new BigDecimal("0.010");   // 1.0%
    private static final BigDecimal VEHICLE_BASE_RATE = new BigDecimal("0.020");  // 2.0%
    private static final BigDecimal PROPERTY_BASE_RATE = new BigDecimal("0.008"); // 0.8%
    private static final BigDecimal TRAVEL_BASE_RATE = new BigDecimal("0.030");   // 3.0%

    // Risk multipliers
    private static final BigDecimal LOW_MULTIPLIER = BigDecimal.ONE;              // 1.0x
    private static final BigDecimal MEDIUM_MULTIPLIER = new BigDecimal("1.5");   // 1.5x
    private static final BigDecimal HIGH_MULTIPLIER = new BigDecimal("2.0");     // 2.0x

    /**
     * Creates a new insurance policy for a customer.
     *
     * FLOW:
     * 1. Validate customer exists
     * 2. Validate policy dates (end > start)
     * 3. Calculate premium based on type + risk level
     * 4. Generate unique policy number
     * 5. Save with PENDING status
     * 6. Return enriched response
     *
     * @param request Policy creation details
     * @return PolicyResponse with generated policy number and calculated premium
     */
    public PolicyResponse createPolicy(PolicyRequest request) {
        log.info("Creating policy for customer: {}", request.getCustomerId());

        // Validate customer exists
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        // Validate dates
        if (!request.getEndDate().isAfter(request.getStartDate())) {
            throw new BusinessRuleException("Policy end date must be after start date.");
        }

        // Calculate premium amount
        BigDecimal premiumAmount = calculatePremium(
                request.getCoverageAmount(),
                request.getPolicyType(),
                customer,
                request.getPremiumFrequency()
        );

        // Build policy entity
        Policy policy = policyMapper.toEntity(request);
        policy.setCustomer(customer);
        policy.setPolicyNumber(generatePolicyNumber());
        policy.setPremiumAmount(premiumAmount);
        policy.setStatus(PolicyStatus.PENDING);

        Policy savedPolicy = policyRepository.save(policy);
        log.info("Policy created: {} for customer: {}", savedPolicy.getPolicyNumber(), customer.getId());

        return enrichWithClaimCounts(policyMapper.toResponse(savedPolicy));
    }

    /**
     * Retrieves a single policy by ID.
     *
     * @param policyId Policy ID
     * @return PolicyResponse
     */
    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", policyId));
        return enrichWithClaimCounts(policyMapper.toResponse(policy));
    }

    /**
     * Gets all policies with optional search and pagination.
     *
     * @param searchTerm Optional filter by policy number, name, or customer name
     * @param pageable   Pagination parameters
     * @return Page of PolicyResponse
     */
    @Transactional(readOnly = true)
    public Page<PolicyResponse> getAllPolicies(String searchTerm, Pageable pageable) {
        Page<Policy> policies = (searchTerm != null && !searchTerm.isBlank())
                ? policyRepository.searchPolicies(searchTerm.trim(), pageable)
                : policyRepository.findAll(pageable);

        return policies.map(p -> enrichWithClaimCounts(policyMapper.toResponse(p)));
    }

    /**
     * Gets all policies for a specific customer (paginated).
     */
    @Transactional(readOnly = true)
    public Page<PolicyResponse> getPoliciesByCustomer(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "id", customerId);
        }
        return policyRepository.findByCustomerId(customerId, pageable)
                .map(p -> enrichWithClaimCounts(policyMapper.toResponse(p)));
    }

    /**
     * Updates a policy's basic details (not premium — recalculated automatically).
     *
     * @param policyId Policy ID to update
     * @param request  Updated policy details
     * @return Updated PolicyResponse
     */
    public PolicyResponse updatePolicy(Long policyId, PolicyRequest request) {
        log.info("Updating policy: {}", policyId);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", policyId));

        // Cannot update CANCELLED or EXPIRED policies
        if (PolicyStatus.CANCELLED.equals(policy.getStatus())
                || PolicyStatus.EXPIRED.equals(policy.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot update a " + policy.getStatus() + " policy."
            );
        }

        // Validate dates
        LocalDate newStart = request.getStartDate() != null ? request.getStartDate() : policy.getStartDate();
        LocalDate newEnd = request.getEndDate() != null ? request.getEndDate() : policy.getEndDate();
        if (!newEnd.isAfter(newStart)) {
            throw new BusinessRuleException("Policy end date must be after start date.");
        }

        // Recalculate premium if coverage amount or type changed
        Customer customer = policy.getCustomer();
        policyMapper.updateEntityFromRequest(request, policy);

        // Recalculate premium with updated values
        BigDecimal newPremium = calculatePremium(
                policy.getCoverageAmount(),
                policy.getPolicyType(),
                customer,
                policy.getPremiumFrequency()
        );
        policy.setPremiumAmount(newPremium);

        Policy updatedPolicy = policyRepository.save(policy);
        return enrichWithClaimCounts(policyMapper.toResponse(updatedPolicy));
    }

    /**
     * Changes a policy's status (ACTIVATE, CANCEL, etc.).
     * Used by agents/admins to approve pending policies or cancel active ones.
     *
     * @param policyId  Policy ID
     * @param newStatus The new status to set
     * @return Updated PolicyResponse
     */
    public PolicyResponse updatePolicyStatus(Long policyId, PolicyStatus newStatus) {
        log.info("Updating policy {} status to {}", policyId, newStatus);

        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", policyId));

        // Validate valid transitions
        validateStatusTransition(policy.getStatus(), newStatus);

        policy.setStatus(newStatus);
        return enrichWithClaimCounts(policyMapper.toResponse(policyRepository.save(policy)));
    }

    /**
     * Deletes a policy. Only allowed for PENDING or CANCELLED policies.
     * Active/expired policies with claim history should not be deleted.
     *
     * @param policyId Policy ID to delete
     */
    public void deletePolicy(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", policyId));

        if (PolicyStatus.ACTIVE.equals(policy.getStatus())) {
            throw new BusinessRuleException(
                    "Cannot delete an ACTIVE policy. Cancel it first."
            );
        }

        policyRepository.delete(policy);
        log.info("Policy {} deleted", policyId);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PREMIUM CALCULATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Core premium calculation method.
     *
     * INTERVIEW TIP: Why BigDecimal operations?
     * BigDecimal is immutable — arithmetic creates new instances.
     * Always use BigDecimal.multiply(), not * operator (which would use double).
     * RoundingMode.HALF_UP = standard rounding (0.5 rounds up, as in school math).
     *
     * @param coverageAmount   Policy coverage amount
     * @param policyType       Type of insurance (affects base rate)
     * @param customer         The customer (for risk level lookup)
     * @param premiumFrequency MONTHLY, QUARTERLY, SEMI_ANNUAL, ANNUAL
     * @return Calculated premium for the given frequency period
     */
    public BigDecimal calculatePremium(
            BigDecimal coverageAmount,
            PolicyType policyType,
            Customer customer,
            String premiumFrequency
    ) {
        // Step 1: Get base annual rate for policy type
        BigDecimal baseRate = getBaseRate(policyType);

        // Step 2: Calculate annual premium before risk adjustment
        // annual = coverageAmount × baseRate
        BigDecimal annualBasePremium = coverageAmount.multiply(baseRate);

        // Step 3: Apply risk multiplier based on customer's risk level
        BigDecimal riskMultiplier = getRiskMultiplier(customer);
        BigDecimal annualPremium = annualBasePremium.multiply(riskMultiplier);

        // Step 4: Convert to the requested frequency
        BigDecimal premiumAmount = convertToFrequency(annualPremium, premiumFrequency);

        return premiumAmount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Returns the annual base rate as a decimal for the given policy type.
     */
    private BigDecimal getBaseRate(PolicyType policyType) {
        return switch (policyType) {
            case LIFE -> LIFE_BASE_RATE;
            case HEALTH -> HEALTH_BASE_RATE;
            case VEHICLE -> VEHICLE_BASE_RATE;
            case PROPERTY -> PROPERTY_BASE_RATE;
            case TRAVEL -> TRAVEL_BASE_RATE;
        };
    }

    /**
     * Returns the risk multiplier based on the customer's current risk assessment.
     * If no assessment exists yet, defaults to MEDIUM multiplier (conservative).
     */
    private BigDecimal getRiskMultiplier(Customer customer) {
        return riskAssessmentRepository.findByCustomerId(customer.getId())
                .map(assessment -> switch (assessment.getRiskLevel()) {
                    case LOW -> LOW_MULTIPLIER;
                    case MEDIUM -> MEDIUM_MULTIPLIER;
                    case HIGH -> HIGH_MULTIPLIER;
                })
                .orElse(MEDIUM_MULTIPLIER); // default to MEDIUM if unassessed
    }

    /**
     * Converts an annual premium to the requested payment frequency.
     *
     * QUARTERLY: pay 4 times/year → divide by 4
     * SEMI_ANNUAL: pay 2 times/year → divide by 2
     * ANNUAL: pay once/year → no change
     * MONTHLY: pay 12 times/year → divide by 12
     */
    private BigDecimal convertToFrequency(BigDecimal annualPremium, String frequency) {
        return switch (frequency.toUpperCase()) {
            case "MONTHLY" -> annualPremium.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
            case "QUARTERLY" -> annualPremium.divide(BigDecimal.valueOf(4), 10, RoundingMode.HALF_UP);
            case "SEMI_ANNUAL" -> annualPremium.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
            case "ANNUAL" -> annualPremium;
            default -> annualPremium.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        };
    }

    // ════════════════════════════════════════════════════════════════════════
    // POLICY NUMBER GENERATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Generates a unique policy number in format: POL-YYYYMMDD-XXXXX
     * Example: POL-20240515-00042
     *
     * Uses a simple random suffix. In production, use a DB sequence or UUID.
     */
    private String generatePolicyNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = new Random().nextInt(99999) + 1;
        String policyNumber = String.format("POL-%s-%05d", dateStr, randomSuffix);

        // Ensure uniqueness (retry if collision)
        int attempts = 0;
        while (policyRepository.findByPolicyNumber(policyNumber).isPresent() && attempts < 10) {
            randomSuffix = new Random().nextInt(99999) + 1;
            policyNumber = String.format("POL-%s-%05d", dateStr, randomSuffix);
            attempts++;
        }

        return policyNumber;
    }

    // ════════════════════════════════════════════════════════════════════════
    // SCHEDULED POLICY EXPIRY JOB
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Automatically expires policies whose end date has passed.
     * Runs daily at midnight (00:00 UTC).
     *
     * INTERVIEW TIP: @Scheduled cron format:
     * "0 0 0 * * *" = second=0, minute=0, hour=0, day=any, month=any, weekday=any
     * → runs at midnight every day
     *
     * @EnableScheduling on the main class enables this.
     * @Transactional: bulk UPDATE runs in a single transaction.
     *
     * PRODUCTION NOTE: For multi-instance deployments (K8s, Docker Swarm),
     * use ShedLock or Quartz to prevent multiple instances from running
     * the same scheduled job simultaneously (race condition).
     */
    @Scheduled(cron = "0 0 0 * * *")  // midnight daily
    @Transactional
    public void expireOverduePolicies() {
        int expiredCount = policyRepository.expireActivePolicies(LocalDate.now());
        if (expiredCount > 0) {
            log.info("Scheduled job: expired {} policies that passed their end date", expiredCount);
        }
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────

    /**
     * Validates that the requested status transition is allowed.
     * Prevents invalid transitions like: EXPIRED → ACTIVE.
     */
    private void validateStatusTransition(PolicyStatus current, PolicyStatus target) {
        boolean invalid = switch (current) {
            case PENDING -> !target.equals(PolicyStatus.ACTIVE) && !target.equals(PolicyStatus.CANCELLED);
            case ACTIVE -> !target.equals(PolicyStatus.CANCELLED) && !target.equals(PolicyStatus.EXPIRED);
            case EXPIRED, CANCELLED -> true;  // terminal states, no further transitions allowed
        };

        if (invalid) {
            throw new BusinessRuleException(
                    "Invalid status transition: " + current + " → " + target
            );
        }
    }

    /**
     * Enriches PolicyResponse with claim count statistics.
     */
    private PolicyResponse enrichWithClaimCounts(PolicyResponse response) {
        if (response.getId() != null) {
            response.setTotalClaims(claimRepository.findByPolicyId(response.getId()).size());
            response.setApprovedClaims(claimRepository.countApprovedClaimsByCustomer(response.getCustomerId()));
        }
        return response;
    }
}
