package com.nnipa.authz.repository;

import com.nnipa.authz.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for Permission entity operations.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByResourceTypeAndAction(String resourceType, String action);

    List<Permission> findByResourceType(String resourceType);

    List<Permission> findByIsSystemTrueAndIsActiveTrue();

    @Query("SELECT DISTINCT p FROM Permission p " +
            "JOIN RolePermission rp ON p.id = rp.permission.id " +
            "JOIN UserRole ur ON rp.role.id = ur.role.id " +
            "WHERE ur.userId = :userId AND ur.tenantId = :tenantId " +
            "AND ur.isActive = true AND p.isActive = true")
    Set<Permission> findUserPermissions(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );

    @Query("SELECT p FROM Permission p WHERE p.resourceType IN :resourceTypes AND p.isActive = true")
    List<Permission> findByResourceTypes(@Param("resourceTypes") List<String> resourceTypes);

    @Query("SELECT p FROM Permission p WHERE p.requiresMfa = true AND p.isActive = true")
    List<Permission> findMfaRequiredPermissions();

    boolean existsByResourceTypeAndAction(String resourceType, String action);
}