package com.nnipa.authz.repository;

import com.nnipa.authz.entity.Resource;
import com.nnipa.authz.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Resource entity operations.
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Optional<Resource> findByResourceIdentifier(String resourceIdentifier);

    List<Resource> findByTenantIdAndResourceType(UUID tenantId, ResourceType resourceType);

    List<Resource> findByTenantIdAndIsActiveTrue(UUID tenantId);

    List<Resource> findByParentResourceId(UUID parentResourceId);

    @Query("SELECT r FROM Resource r " +
            "LEFT JOIN FETCH r.policies " +
            "WHERE r.resourceIdentifier = :identifier")
    Optional<Resource> findByIdentifierWithPolicies(@Param("identifier") String identifier);

    @Query("SELECT r FROM Resource r " +
            "WHERE r.ownerId = :ownerId AND r.tenantId = :tenantId")
    List<Resource> findByOwnerIdAndTenantId(
            @Param("ownerId") UUID ownerId,
            @Param("tenantId") UUID tenantId
    );

    @Query("SELECT r FROM Resource r " +
            "WHERE r.isPublic = true AND r.isActive = true")
    List<Resource> findPublicResources();

    boolean existsByResourceIdentifier(String resourceIdentifier);
}