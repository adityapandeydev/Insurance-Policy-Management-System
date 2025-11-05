package com.insurance.mapper;

import com.insurance.dto.request.ClaimRequest;
import com.insurance.dto.response.ClaimResponse;
import com.insurance.entity.Claim;
import org.mapstruct.*;

/**
 * MapStruct mapper for Claim entity ↔ DTO conversions.
 * Maps nested policy and customer fields to flat ClaimResponse structure.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClaimMapper {

    @Mapping(source = "policy.id", target = "policyId")
    @Mapping(source = "policy.policyNumber", target = "policyNumber")
    @Mapping(source = "policy.policyName", target = "policyName")
    @Mapping(source = "policy.coverageAmount", target = "policyCoverageAmount")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.user.email", target = "customerEmail")
    @Mapping(target = "customerName", expression = "java(claim.getCustomer().getFullName())")
    ClaimResponse toResponse(Claim claim);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "claimNumber", ignore = true)  // generated in service
    @Mapping(target = "status", ignore = true)        // defaults to PENDING
    @Mapping(target = "reviewNotes", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "policy", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Claim toEntity(ClaimRequest request);
}
