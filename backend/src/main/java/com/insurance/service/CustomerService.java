package com.insurance.service;

import com.insurance.dto.request.CustomerRequest;
import com.insurance.dto.response.CustomerResponse;
import com.insurance.entity.Customer;
import com.insurance.entity.User;
import com.insurance.enums.Role;
import com.insurance.exception.BusinessRuleException;
import com.insurance.exception.ResourceNotFoundException;
import com.insurance.exception.UnauthorizedException;
import com.insurance.mapper.CustomerMapper;
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

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                       CUSTOMER SERVICE                                  ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Handles all customer profile management operations.                    ║
 * ║                                                                          ║
 * ║  BUSINESS RULES:                                                         ║
 * ║  • Each User can have only ONE Customer profile                         ║
 * ║  • National ID must be unique across all customers                      ║
 * ║  • CUSTOMER role users can only view/update their own profile           ║
 * ║  • Only ADMIN can delete customers                                       ║
 * ║  • Deleting a customer cascades to policies, claims, and risk profile   ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: @Transactional at service level                         ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Service methods are the transactional boundary.                        ║
 * ║  NOT at repository level (repos just provide DB operations).            ║
 * ║  NOT at controller level (too high, should not span business logic).   ║
 * ║  Service is the right layer for @Transactional because:                ║
 * ║  • It contains the business logic                                        ║
 * ║  • Multiple repository calls should be in ONE transaction               ║
 * ║  • If any step fails, ALL changes are rolled back                       ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional  // All methods in this service run within a database transaction
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final CustomerMapper customerMapper;

    /**
     * Creates a customer profile for an existing user.
     *
     * FLOW:
     * 1. Load the user by ID
     * 2. Validate: user must be CUSTOMER role
     * 3. Validate: user doesn't already have a customer profile
     * 4. Validate: national ID uniqueness (if provided)
     * 5. Map request → entity, set user reference
     * 6. Save and return response DTO
     *
     * @param userId  The user ID to create a profile for
     * @param request Customer profile details
     * @return CustomerResponse with the created profile
     */
    public CustomerResponse createCustomerProfile(Long userId, CustomerRequest request) {
        log.info("Creating customer profile for userId: {}", userId);

        // Load the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate role: only CUSTOMER role users get a customer profile
        if (!Role.ROLE_CUSTOMER.equals(user.getRole())) {
            throw new BusinessRuleException(
                    "Customer profiles can only be created for users with CUSTOMER role."
            );
        }

        // Validate: user doesn't already have a customer profile
        if (customerRepository.findByUserId(userId).isPresent()) {
            throw new BusinessRuleException(
                    "A customer profile already exists for user ID: " + userId
            );
        }

        // Validate national ID uniqueness
        if (request.getNationalId() != null && !request.getNationalId().isBlank()
                && customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessRuleException(
                    "A customer with national ID '" + request.getNationalId() + "' already exists."
            );
        }

        // Map DTO → Entity and set user reference
        Customer customer = customerMapper.toEntity(request);
        customer.setUser(user);

        Customer savedCustomer = customerRepository.save(customer);
        log.info("Customer profile created with id: {}", savedCustomer.getId());

        return enrichWithCounts(customerMapper.toResponse(savedCustomer));
    }

    /**
     * Retrieves a customer profile by ID.
     * CUSTOMER role users can only access their own profile (enforced here).
     *
     * @param customerId The customer ID to retrieve
     * @return CustomerResponse
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Authorization: CUSTOMER can only see their own profile
        enforceCustomerSelfAccess(customer);

        return enrichWithCounts(customerMapper.toResponse(customer));
    }

    /**
     * Retrieves the currently authenticated customer's own profile.
     * Used by CUSTOMER role users to view their profile via /customers/me endpoint.
     *
     * @return CustomerResponse for the current user
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCurrentCustomerProfile() {
        User currentUser = getCurrentAuthenticatedUser();

        Customer customer = customerRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile", "userId", currentUser.getId()
                ));

        return enrichWithCounts(customerMapper.toResponse(customer));
    }

    /**
     * Retrieves all customers with pagination and optional search.
     * ADMIN/AGENT only endpoint.
     *
     * INTERVIEW TIP: Pagination parameters
     * PageRequest.of(page, size, Sort.by("createdAt").descending())
     * → SELECT * FROM customers ORDER BY created_at DESC LIMIT size OFFSET (page * size)
     *
     * @param searchTerm Optional search term (name, email, phone)
     * @param pageable   Pagination and sorting parameters
     * @return Page of CustomerResponse
     */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(String searchTerm, Pageable pageable) {
        Page<Customer> customers;

        if (searchTerm != null && !searchTerm.isBlank()) {
            customers = customerRepository.searchCustomers(searchTerm.trim(), pageable);
        } else {
            customers = customerRepository.findAll(pageable);
        }

        // Map each Customer entity to CustomerResponse and enrich with counts
        return customers.map(customer -> enrichWithCounts(customerMapper.toResponse(customer)));
    }

    /**
     * Updates a customer profile.
     * CUSTOMER role can only update their own profile.
     *
     * @param customerId Customer ID to update
     * @param request    Updated customer details
     * @return Updated CustomerResponse
     */
    public CustomerResponse updateCustomer(Long customerId, CustomerRequest request) {
        log.info("Updating customer with id: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Authorization: customer can only update their own profile
        enforceCustomerSelfAccess(customer);

        // National ID uniqueness check (only if changing it)
        if (request.getNationalId() != null
                && !request.getNationalId().equals(customer.getNationalId())
                && customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessRuleException(
                    "National ID '" + request.getNationalId() + "' is already registered."
            );
        }

        // Update only the fields provided in the request (PATCH semantics via MapStruct)
        customerMapper.updateEntityFromRequest(request, customer);
        Customer updatedCustomer = customerRepository.save(customer);

        log.info("Customer {} updated successfully", customerId);
        return enrichWithCounts(customerMapper.toResponse(updatedCustomer));
    }

    /**
     * Soft-deletes a customer profile by disabling their user account.
     * Hard delete is cascaded at DB level (policies, claims, risk deleted too).
     * ADMIN only operation.
     *
     * INTERVIEW TIP: Hard delete vs Soft delete
     * Hard delete: DELETE FROM customers WHERE id = ?
     *   → Data is gone forever. If it's referenced by audit logs, those break.
     * Soft delete: UPDATE users SET enabled = false WHERE id = ?
     *   → Data preserved for auditing. User can't log in. Can be restored.
     * Insurance systems often require soft delete for regulatory compliance.
     *
     * @param customerId Customer ID to delete
     */
    public void deleteCustomer(Long customerId) {
        log.info("Deleting customer with id: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Disable the user account (soft delete)
        User user = customer.getUser();
        user.setEnabled(false);
        userRepository.save(user);

        log.info("Customer {} soft-deleted (user account disabled)", customerId);
    }

    // ─── PRIVATE HELPER METHODS ───────────────────────────────────────────

    /**
     * Enriches a CustomerResponse with policy and claim counts.
     * Uses separate COUNT queries instead of loading full collections.
     *
     * INTERVIEW TIP: Why COUNT queries instead of List size?
     * list.size() on a LAZY collection triggers: SELECT * FROM policies WHERE customer_id = ?
     * This loads ALL policy objects into memory just to count them.
     * COUNT query: SELECT COUNT(*) FROM policies WHERE customer_id = ?
     * Only returns a number, much more efficient for large datasets.
     */
    private CustomerResponse enrichWithCounts(CustomerResponse response) {
        if (response.getId() != null) {
            response.setTotalPolicies(policyRepository.countByCustomerId(response.getId()));
            response.setTotalClaims(claimRepository.countByCustomerId(response.getId()));
        }
        return response;
    }

    /**
     * Enforces that a CUSTOMER role user can only access their own data.
     * ADMIN and AGENT roles bypass this check (they can access any customer).
     *
     * INTERVIEW TIP: This is attribute-based access control (ABAC) —
     * the decision depends on the RESOURCE (customer.user.email) matching
     * the PRINCIPAL (currently logged-in user's email).
     * Spring Security's @PreAuthorize can also do this:
     * @PreAuthorize("#customer.user.email == authentication.name || hasRole('ADMIN')")
     * but implementing in the service gives us more control.
     */
    private void enforceCustomerSelfAccess(Customer customer) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserEmail = auth.getName();

        boolean isAdminOrAgent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                            || a.getAuthority().equals("ROLE_AGENT"));

        if (!isAdminOrAgent) {
            // CUSTOMER can only access their own profile
            if (!customer.getUser().getEmail().equals(currentUserEmail)) {
                throw new UnauthorizedException(
                        "Access denied. You can only view your own profile."
                );
            }
        }
    }

    /**
     * Retrieves the currently authenticated User from SecurityContextHolder.
     * The User is stored as the principal during JWT authentication.
     */
    private User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
