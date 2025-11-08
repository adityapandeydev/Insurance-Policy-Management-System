package com.insurance.repository;

import com.insurance.entity.RiskAssessment;
import com.insurance.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RiskAssessment repository for one-to-one customer risk profile management.
 */
@Repository
public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {

    /**
     * Finds the risk assessment for a specific customer.
     * Returns Optional since a customer may not have been assessed yet.
     */
    Optional<RiskAssessment> findByCustomerId(Long customerId);

    /**
     * Checks if a risk assessment exists for this customer.
     * Used to decide whether to INSERT or UPDATE.
     */
    boolean existsByCustomerId(Long customerId);

    /**
     * Counts customers by risk level.
     * Dashboard: "How many HIGH risk customers do we have?"
     */
    long countByRiskLevel(RiskLevel riskLevel);
}
