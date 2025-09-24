package com.nnipa.authz.repository;

import com.nnipa.authz.entity.Resource;
import com.nnipa.authz.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    // Identifier-based queries
    Optional<Resource> findByResourceIdentifier(String resourceIdentifier);

    Optional<Resource> findByResourceIdentifierAndTenantId(String resourceIdentifier, UUID tenantId);

    @Query("SELECT r FROM Resource r LEFT JOIN FETCH r.policies WHERE r.resourceIdentifier = :identifier")
    Optional<Resource> findByIdentifierWithPolicies(@Param("identifier") String identifier);

    boolean existsByResourceIdentifierAndTenantId(String resourceIdentifier, UUID tenantId);

    // Tenant queries
    List<Resource> findByTenantId(UUID tenantId);

    Page<Resource> findByTenantId(UUID tenantId, Pageable pageable);

    List<Resource> findByTenantIdAndResourceType(UUID tenantId, ResourceType resourceType);

    Page<Resource> findByTenantIdAndResourceType(UUID tenantId, ResourceType resourceType, Pageable pageable);

    // Owner queries
    List<Resource> findByOwnerId(UUID ownerId);

    List<Resource> findByOwnerIdAndTenantId(UUID ownerId, UUID tenantId);

    // Hierarchy queries
    List<Resource> findByParentResourceId(UUID parentResourceId);

    @Query("SELECT r FROM Resource r WHERE r.parentResource IS NULL AND r.tenantId = :tenantId")
    List<Resource> findRootResourcesByTenant(@Param("tenantId") UUID tenantId);

    // Public resources
    List<Resource> findByIsPublicTrue();

    List<Resource> findByTenantIdAndIsPublicTrue(UUID tenantId);

    // Complex filtering
    @Query("SELECT r FROM Resource r WHERE " +
            "(:tenantId IS NULL OR r.tenantId = :tenantId) AND " +
            "(:resourceType IS NULL OR r.resourceType = :resourceType) AND " +
            "(:ownerId IS NULL OR r.ownerId = :ownerId) AND " +
            "(:isPublic IS NULL OR r.isPublic = :isPublic) AND " +
            "(:searchTerm IS NULL OR LOWER(r.resourceIdentifier) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Resource> findWithFilters(@Param("tenantId") UUID tenantId,
                                   @Param("resourceType") ResourceType resourceType,
                                   @Param("ownerId") UUID ownerId,
                                   @Param("isPublic") Boolean isPublic,
                                   @Param("searchTerm") String searchTerm,
                                   Pageable pageable);

    // Resource with policies
    @Query("SELECT DISTINCT r FROM Resource r LEFT JOIN FETCH r.policies WHERE r.id = :resourceId")
    Optional<Resource> findByIdWithPolicies(@Param("resourceId") UUID resourceId);

    // Count queries
    long countByTenantId(UUID tenantId);

    long countByResourceType(ResourceType resourceType);

    long countByTenantIdAndResourceType(UUID tenantId, ResourceType resourceType);

    // Batch operations
    @Query("SELECT r FROM Resource r WHERE r.resourceIdentifier IN :identifiers")
    List<Resource> findByResourceIdentifierIn(@Param("identifiers") List<String> identifiers);
}