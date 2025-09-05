package com.nnipa.authz.repository;

import com.nnipa.authz.entity.Policy;
import com.nnipa.authz.enums.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Policy entity operations.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    Optional<Policy> findByNameAndTenantId(String name, UUID tenantId);

    List<Policy> findByTenantIdAndIsActiveTrue(UUID tenantId);

    List<Policy> findByPolicyTypeAndTenantId(PolicyType policyType, UUID tenantId);

    @Query("SELECT p FROM Policy p " +
            "LEFT JOIN FETCH p.permissions " +
            "WHERE p.id = :id")
    Optional<Policy> findByIdWithPermissions(@Param("id") UUID id);

    @Query("SELECT p FROM Policy p " +
            "JOIN p.resources r " +
            "WHERE r.id = :resourceId AND p.isActive = true " +
            "ORDER BY p.priority DESC")
    List<Policy> findByResourceId(@Param("resourceId") UUID resourceId);

    @Query("SELECT p FROM Policy p " +
            "WHERE p.tenantId = :tenantId " +
            "AND p.isActive = true " +
            "AND (p.startDate IS NULL OR p.startDate <= CURRENT_TIMESTAMP) " +
            "AND (p.endDate IS NULL OR p.endDate >= CURRENT_TIMESTAMP) " +
            "ORDER BY p.priority DESC")
    List<Policy> findActivePoliciesForTenant(@Param("tenantId") UUID tenantId);

    boolean existsByNameAndTenantId(String name, UUID tenantId);
}