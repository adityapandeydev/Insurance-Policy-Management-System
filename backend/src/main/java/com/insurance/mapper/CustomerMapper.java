package com.insurance.mapper;

import com.insurance.dto.request.CustomerRequest;
import com.insurance.dto.response.CustomerResponse;
import com.insurance.entity.Customer;
import org.mapstruct.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                       CUSTOMER MAPPER (MapStruct)                       ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  MapStruct generates the implementation of this interface at            ║
 * ║  compile time. The generated class (CustomerMapperImpl.java) will       ║
 * ║  be placed in target/generated-sources/annotations/                     ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: How MapStruct works                                     ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  1. You define an interface with mapping methods                        ║
 * ║  2. MapStruct reads source and target types at compile time             ║
 * ║  3. It generates a concrete implementation class with plain Java code:  ║
 * ║       CustomerResponse toResponse(Customer customer) {                  ║
 * ║           if (customer == null) return null;                            ║
 * ║           CustomerResponse response = new CustomerResponse();           ║
 * ║           response.setId(customer.getId());                             ║
 * ║           response.setFirstName(customer.getUser().getFirstName()); ... ║
 * ║           return response;                                               ║
 * ║       }                                                                  ║
 * ║  4. Zero reflection → extremely fast (benchmark: 10x faster than       ║
 * ║     ModelMapper or Dozer which use reflection at runtime)               ║
 * ║                                                                          ║
 * ║  componentModel = "spring": Generated class annotated with @Component  ║
 * ║  → Spring auto-detects and injects it anywhere with @Autowired         ║
 * ║                                                                          ║
 * ║  @Mapping annotations:                                                  ║
 * ║  source = "user.firstName" → navigates nested properties via dot notation ║
 * ║  expression = "java(...)"  → runs arbitrary Java code for complex fields ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    /**
     * Maps Customer entity → CustomerResponse DTO.
     *
     * @Mapping: explicitly maps nested entity fields to flat DTO fields.
     * "user.firstName" → navigates Customer.user.firstName (requires getUser() getter)
     *
     * INTERVIEW TIP: If field names match, MapStruct maps them automatically.
     * Only specify @Mapping for:
     * 1. Different field names
     * 2. Nested properties (customer.user.email → customerResponse.email)
     * 3. Computed values (age = calculated from dateOfBirth)
     * 4. Ignored fields
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.role", target = "role")
    @Mapping(target = "age", expression = "java(customer.getAge())")
    @Mapping(target = "totalPolicies", ignore = true)   // set in service layer (count query)
    @Mapping(target = "totalClaims", ignore = true)      // set in service layer (count query)
    CustomerResponse toResponse(Customer customer);

    /**
     * Maps CustomerRequest DTO → Customer entity (for CREATE operations).
     * The 'user' field on Customer is set separately in the service layer.
     *
     * @Mapping(target = "user", ignore = true): User association is handled
     * in the service layer, not by the mapper. The mapper shouldn't
     * make repository calls.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "policies", ignore = true)
    @Mapping(target = "claims", ignore = true)
    @Mapping(target = "riskAssessment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerRequest request);

    /**
     * Updates an existing Customer entity from a request DTO (for UPDATE operations).
     *
     * @MappingTarget: The annotated parameter is the target to update in place.
     * Instead of creating a new Customer, this method updates existing fields.
     *
     * INTERVIEW TIP: This is the "partial update" pattern in MapStruct.
     * Use @BeanMapping(nullValuePropertyMappingStrategy = IGNORE) to skip
     * null fields in the request (implements PATCH semantics where only
     * provided fields are updated).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "policies", ignore = true)
    @Mapping(target = "claims", ignore = true)
    @Mapping(target = "riskAssessment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
