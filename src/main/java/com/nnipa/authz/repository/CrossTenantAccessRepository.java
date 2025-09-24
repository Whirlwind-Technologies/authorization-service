package com.nnipa.authz.repository;

import com.nnipa.authz.entity.CrossTenantAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CrossTenantAccessRepository extends JpaRepository<CrossTenantAccess, UUID> {

    @Query("SELECT cta FROM CrossTenantAccess cta WHERE " +
            "cta.sourceTenantId = :sourceTenantId AND " +
            "cta.targetTenantId = :targetTenantId AND " +
            "cta.resourceType = :resourceType AND " +
            "cta.isActive = true")
    List<CrossTenantAccess> findActiveAccessByTenants(@Param("sourceTenantId") UUID sourceTenantId,
                                                      @Param("targetTenantId") UUID targetTenantId,
                                                      @Param("resourceType") String resourceType);

    @Query("SELECT cta FROM CrossTenantAccess cta WHERE " +
            "(cta.sourceTenantId = :tenantId OR cta.targetTenantId = :tenantId) AND " +
            "cta.isActive = true")
    List<CrossTenantAccess> findActiveByTenant(@Param("tenantId") UUID tenantId);

    boolean existsBySourceTenantIdAndTargetTenantIdAndResourceType(UUID sourceTenantId,
                                                                   UUID targetTenantId,
                                                                   String resourceType);


    @Query("SELECT COUNT(cta) FROM CrossTenantAccess cta WHERE cta.isActive = true")
    long countActive();
}