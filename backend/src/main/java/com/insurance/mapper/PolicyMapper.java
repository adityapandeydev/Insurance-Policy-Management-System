package com.insurance.mapper;

import com.insurance.dto.request.PolicyRequest;
import com.insurance.dto.response.PolicyResponse;
import com.insurance.entity.Policy;
import org.mapstruct.*;

/**
 * MapStruct mapper for Policy entity ↔ DTO conversions.
 *
 * Note the customer fields are mapped from the nested customer.user object.
 * MapStruct handles nested dot-notation paths automatically.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PolicyMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.user.firstName", target = "customerName",
             defaultExpression = "java(policy.getCustomer().getFullName())")
    @Mapping(source = "customer.user.email", target = "customerEmail")
    @Mapping(target = "totalClaims", ignore = true)    // set in service
    @Mapping(target = "approvedClaims", ignore = true) // set in service
    PolicyResponse toResponse(Policy policy);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "policyNumber", ignore = true)   // generated in service
    @Mapping(target = "premiumAmount", ignore = true)  // calculated in service
    @Mapping(target = "customer", ignore = true)       // set in service
    @Mapping(target = "claims", ignore = true)
    @Mapping(target = "status", ignore = true)         // defaults to PENDING
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Policy toEntity(PolicyRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "policyNumber", ignore = true)
    @Mapping(target = "premiumAmount", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "claims", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(PolicyRequest request, @MappingTarget Policy policy);
}
