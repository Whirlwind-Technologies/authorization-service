package com.nnipa.authz.repository;

import com.nnipa.authz.entity.Policy;
import com.nnipa.authz.enums.PolicyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    // Basic queries
    Optional<Policy> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Policy> findByTenantId(UUID tenantId);

    Page<Policy> findByTenantId(UUID tenantId, Pageable pageable);

    List<Policy> findByPolicyType(PolicyType policyType);

    boolean existsByNameAndTenantId(String name, UUID tenantId);

    // Active policies
    @Query("SELECT p FROM Policy p WHERE p.tenantId = :tenantId " +
            "AND p.isActive = true " +
            "AND (p.startDate IS NULL OR p.startDate <= :now) " +
            "AND (p.endDate IS NULL OR p.endDate >= :now)")
    List<Policy> findActivePoliciesForTenant(@Param("tenantId") UUID tenantId,
                                             @Param("now") Instant now);

    default List<Policy> findActivePoliciesForTenant(UUID tenantId) {
        return findActivePoliciesForTenant(tenantId, Instant.now());
    }

    // Filtering queries
    @Query("SELECT p FROM Policy p WHERE " +
            "(:tenantId IS NULL OR p.tenantId = :tenantId) AND " +
            "(:policyType IS NULL OR p.policyType = :policyType) AND " +
            "(:isActive IS NULL OR p.isActive = :isActive) AND " +
            "(:searchTerm IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Policy> findWithFilters(@Param("tenantId") UUID tenantId,
                                 @Param("policyType") PolicyType policyType,
                                 @Param("isActive") Boolean isActive,
                                 @Param("searchTerm") String searchTerm,
                                 Pageable pageable);

    // Resource policies
    @Query("SELECT p FROM Policy p " +
            "JOIN p.resources r " +
            "WHERE r.id = :resourceId AND p.isActive = true")
    List<Policy> findByResourceId(@Param("resourceId") UUID resourceId);

    // Priority-ordered policies
    @Query("SELECT p FROM Policy p WHERE p.tenantId = :tenantId " +
            "AND p.isActive = true ORDER BY p.priority DESC")
    List<Policy> findActivePoliciesOrderedByPriority(@Param("tenantId") UUID tenantId);

    // Update operations
    @Modifying
    @Query("UPDATE Policy p SET p.isActive = false WHERE p.id = :policyId")
    void deactivatePolicy(@Param("policyId") UUID policyId);

    @Modifying
    @Query("UPDATE Policy p SET p.isActive = true WHERE p.id = :policyId")
    void activatePolicy(@Param("policyId") UUID policyId);

    // Count queries
    long countByTenantId(UUID tenantId);

    long countByTenantIdAndIsActiveTrue(UUID tenantId);

    long countByPolicyType(PolicyType policyType);

    // Expired policies cleanup
    @Query("SELECT p FROM Policy p WHERE p.endDate < :now AND p.isActive = true")
    List<Policy> findExpiredPolicies(@Param("now") Instant now);
}