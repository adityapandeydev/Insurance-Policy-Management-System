package com.insurance.service;

import com.insurance.dto.request.ClaimRequest;
import com.insurance.dto.request.ClaimReviewRequest;
import com.insurance.dto.response.ClaimResponse;
import com.insurance.entity.Claim;
import com.insurance.entity.Customer;
import com.insurance.entity.Policy;
import com.insurance.enums.ClaimStatus;
import com.insurance.exception.BusinessRuleException;
import com.insurance.exception.ResourceNotFoundException;
import com.insurance.exception.UnauthorizedException;
import com.insurance.mapper.ClaimMapper;
import com.insurance.repository.ClaimRepository;
import com.insurance.repository.CustomerRepository;
import com.insurance.repository.PolicyRepository;
import com.insurance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                         CLAIM SERVICE                                   ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  BUSINESS RULES ENFORCED:                                               ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  1. Policy must be ACTIVE (not EXPIRED, CANCELLED, or PENDING)         ║
 * ║  2. Claim amount ≤ policy coverage amount                               ║
 * ║  3. Incident date must fall within the policy period (start to end)    ║
 * ║  4. Customer can only submit claims for their OWN policies              ║
 * ║  5. Only ADMIN/AGENT can approve or reject claims                       ║
 * ║  6. Once APPROVED/REJECTED, a claim is in a terminal state (immutable) ║
 * ║  7. Only the customer who submitted can WITHDRAW a PENDING claim        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ClaimMapper claimMapper;

    /**
     * Submits a new insurance claim.
     *
     * INTERVIEW TIP: Note the sequence of validations — each builds on the previous.
     * This is the "fail fast" principle: check the cheapest validations first
     * (field existence) before the expensive ones (business rule checks).
     *
     * @param request Claim submission details
     * @return ClaimResponse with generated claim number
     */
    public ClaimResponse submitClaim(ClaimRequest request) {
        log.info("Submitting claim for policy: {}", request.getPolicyId());

        // ─── RULE 1: Policy must exist ───────────────────────────────────
        Policy policy = policyRepository.findById(request.getPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", request.getPolicyId()));

        // ─── RULE 2: Policy must be ACTIVE and in force ──────────────────
        if (!policy.isActiveAndInForce()) {
            throw new BusinessRuleException(
                    "Claims can only be submitted against ACTIVE policies. " +
                    "Policy '" + policy.getPolicyNumber() + "' is currently " + policy.getStatus() + "."
            );
        }

        // ─── RULE 3: Customer can only claim on their own policies ────────
        Customer currentCustomer = getCurrentCustomer();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrAgent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAdminOrAgent) {
            // CUSTOMER role: verify they own this policy
            if (!policy.getCustomer().getId().equals(currentCustomer.getId())) {
                throw new UnauthorizedException(
                        "You can only submit claims for your own policies."
                );
            }
        }

        // Determine the customer (admin submitting on behalf of a customer uses policy's customer)
        Customer claimCustomer = isAdminOrAgent ? policy.getCustomer() : currentCustomer;

        // ─── RULE 4: Claim amount must not exceed coverage amount ─────────
        if (request.getClaimAmount().compareTo(policy.getCoverageAmount()) > 0) {
            throw new BusinessRuleException(
                    String.format(
                        "Claim amount (%.2f) cannot exceed the policy coverage amount (%.2f).",
                        request.getClaimAmount(),
                        policy.getCoverageAmount()
                    )
            );
        }

        // ─── RULE 5: Incident date must be within the policy period ───────
        LocalDate incidentDate = request.getIncidentDate();
        if (incidentDate.isBefore(policy.getStartDate()) || incidentDate.isAfter(policy.getEndDate())) {
            throw new BusinessRuleException(
                    "Incident date (" + incidentDate + ") must be within the policy period (" +
                    policy.getStartDate() + " to " + policy.getEndDate() + ")."
            );
        }

        // ─── BUILD CLAIM ENTITY ───────────────────────────────────────────
        Claim claim = claimMapper.toEntity(request);
        claim.setClaimNumber(generateClaimNumber());
        claim.setPolicy(policy);
        claim.setCustomer(claimCustomer);
        claim.setStatus(ClaimStatus.PENDING);
        claim.setSubmittedAt(LocalDateTime.now());

        Claim savedClaim = claimRepository.save(claim);
        log.info("Claim submitted: {} for policy: {}", savedClaim.getClaimNumber(), policy.getPolicyNumber());

        return claimMapper.toResponse(savedClaim);
    }

    /**
     * Retrieves a claim by its ID.
     * CUSTOMER: can only view their own claims.
     */
    @Transactional(readOnly = true)
    public ClaimResponse getClaimById(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        enforceClaimOwnershipForCustomer(claim);

        return claimMapper.toResponse(claim);
    }

    /**
     * Gets all claims (admin/agent) or claims for the current customer.
     */
    @Transactional(readOnly = true)
    public Page<ClaimResponse> getAllClaims(ClaimStatus status, Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrAgent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_AGENT"));

        Page<Claim> claims;
        if (isAdminOrAgent) {
            claims = claimRepository.findAll(pageable);
        } else {
            Customer customer = getCurrentCustomer();
            claims = (status != null)
                    ? claimRepository.findByCustomerIdAndStatus(customer.getId(), status, pageable)
                    : claimRepository.findByCustomerId(customer.getId(), pageable);
        }

        return claims.map(claimMapper::toResponse);
    }

    /**
     * Gets all claims for a specific policy.
     */
    @Transactional(readOnly = true)
    public Page<ClaimResponse> getClaimsByPolicy(Long policyId, Pageable pageable) {
        if (!policyRepository.existsById(policyId)) {
            throw new ResourceNotFoundException("Policy", "id", policyId);
        }
        return claimRepository.findByPolicyId(policyId, pageable)
                .map(claimMapper::toResponse);
    }

    /**
     * Moves a PENDING claim to UNDER_REVIEW status.
     * Only ADMIN/AGENT can review claims.
     *
     * @param claimId Claim ID to start reviewing
     * @return Updated ClaimResponse
     */
    public ClaimResponse startClaimReview(Long claimId) {
        log.info("Starting review for claim: {}", claimId);

        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        if (!ClaimStatus.PENDING.equals(claim.getStatus())) {
            throw new BusinessRuleException(
                    "Only PENDING claims can be moved to UNDER_REVIEW. Current status: " + claim.getStatus()
            );
        }

        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        return claimMapper.toResponse(claimRepository.save(claim));
    }

    /**
     * Approves a claim that is PENDING or UNDER_REVIEW.
     * Only ADMIN/AGENT can approve.
     *
     * BUSINESS RULE: Approval is irreversible (terminal state).
     *
     * @param claimId Claim ID to approve
     * @param review  Review notes explaining the approval decision
     * @return Updated ClaimResponse with APPROVED status
     */
    public ClaimResponse approveClaim(Long claimId, ClaimReviewRequest review) {
        log.info("Approving claim: {}", claimId);

        Claim claim = findClaimForReview(claimId);
        claim.setStatus(ClaimStatus.APPROVED);
        claim.setReviewNotes(review.getReviewNotes());
        claim.setReviewedAt(LocalDateTime.now());

        Claim approved = claimRepository.save(claim);
        log.info("Claim {} APPROVED. Notes: {}", claim.getClaimNumber(), review.getReviewNotes());

        return claimMapper.toResponse(approved);
    }

    /**
     * Rejects a claim with mandatory review notes explaining the reason.
     *
     * @param claimId Claim ID to reject
     * @param review  Review notes (REQUIRED for rejection — customer transparency)
     * @return Updated ClaimResponse with REJECTED status
     */
    public ClaimResponse rejectClaim(Long claimId, ClaimReviewRequest review) {
        log.info("Rejecting claim: {}", claimId);

        Claim claim = findClaimForReview(claimId);
        claim.setStatus(ClaimStatus.REJECTED);
        claim.setReviewNotes(review.getReviewNotes());
        claim.setReviewedAt(LocalDateTime.now());

        Claim rejected = claimRepository.save(claim);
        log.info("Claim {} REJECTED. Reason: {}", claim.getClaimNumber(), review.getReviewNotes());

        return claimMapper.toResponse(rejected);
    }

    /**
     * Allows a customer to withdraw their own PENDING claim.
     *
     * @param claimId Claim ID to withdraw
     * @return Updated ClaimResponse with WITHDRAWN status
     */
    public ClaimResponse withdrawClaim(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        // Only the claim's owner can withdraw
        enforceClaimOwnershipForCustomer(claim);

        if (!ClaimStatus.PENDING.equals(claim.getStatus())) {
            throw new BusinessRuleException(
                    "Only PENDING claims can be withdrawn. Current status: " + claim.getStatus()
            );
        }

        claim.setStatus(ClaimStatus.WITHDRAWN);
        return claimMapper.toResponse(claimRepository.save(claim));
    }

    // ─── PRIVATE HELPERS ──────────────────────────────────────────────────

    /**
     * Finds a claim for review, enforcing that it's in a reviewable state.
     */
    private Claim findClaimForReview(Long claimId) {
        Claim claim = claimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "id", claimId));

        if (claim.isTerminalState()) {
            throw new BusinessRuleException(
                    "Claim '" + claim.getClaimNumber() + "' is in terminal state " +
                    claim.getStatus() + " and cannot be modified."
            );
        }
        return claim;
    }

    /**
     * For CUSTOMER role users, ensures they can only access their own claims.
     */
    private void enforceClaimOwnershipForCustomer(Claim claim) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrAgent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAdminOrAgent) {
            String currentUserEmail = auth.getName();
            if (!claim.getCustomer().getUser().getEmail().equals(currentUserEmail)) {
                throw new UnauthorizedException("You can only access your own claims.");
            }
        }
    }

    /**
     * Gets the Customer entity for the currently authenticated user.
     */
    private Customer getCurrentCustomer() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return customerRepository.findByUserEmail(email)
                .orElseThrow(() -> new BusinessRuleException(
                        "No customer profile found for the current user. Please create a profile first."
                ));
    }

    /**
     * Generates a unique claim number: CLM-YYYYMMDD-NNNNN
     * Example: CLM-20240515-00042
     */
    private String generateClaimNumber() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = new Random().nextInt(99999) + 1;
        String claimNumber = String.format("CLM-%s-%05d", dateStr, randomSuffix);

        int attempts = 0;
        while (claimRepository.existsByClaimNumber(claimNumber) && attempts < 10) {
            randomSuffix = new Random().nextInt(99999) + 1;
            claimNumber = String.format("CLM-%s-%05d", dateStr, randomSuffix);
            attempts++;
        }
        return claimNumber;
    }
}
