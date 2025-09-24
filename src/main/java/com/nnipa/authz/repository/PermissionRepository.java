package com.nnipa.authz.repository;

import com.nnipa.authz.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    // Basic queries
    Optional<Permission> findByResourceTypeAndAction(String resourceType, String action);

    boolean existsByResourceTypeAndAction(String resourceType, String action);

    List<Permission> findByResourceType(String resourceType);

    List<Permission> findByAction(String action);

    List<Permission> findByIsSystemTrue();

    List<Permission> findByIsActiveTrue();

    // Filtering queries
    Page<Permission> findByResourceTypeContainingIgnoreCase(String resourceType, Pageable pageable);

    Page<Permission> findByIsSystem(Boolean isSystem, Pageable pageable);

    Page<Permission> findByResourceTypeAndIsActive(String resourceType, Boolean isActive, Pageable pageable);

    // Complex queries
    @Query("SELECT p FROM Permission p WHERE " +
            "(:resourceType IS NULL OR p.resourceType = :resourceType) AND " +
            "(:action IS NULL OR p.action = :action) AND " +
            "(:isSystem IS NULL OR p.isSystem = :isSystem) AND " +
            "(:isActive IS NULL OR p.isActive = :isActive)")
    Page<Permission> findWithFilters(@Param("resourceType") String resourceType,
                                     @Param("action") String action,
                                     @Param("isSystem") Boolean isSystem,
                                     @Param("isActive") Boolean isActive,
                                     Pageable pageable);

    // User permissions through roles
    @Query("SELECT DISTINCT p FROM Permission p " +
            "JOIN RolePermission rp ON rp.permission = p " +
            "JOIN Role r ON rp.role = r " +
            "JOIN UserRole ur ON ur.role = r " +
            "WHERE ur.userId = :userId AND ur.tenantId = :tenantId " +
            "AND ur.isActive = true AND r.isActive = true AND p.isActive = true")
    Set<Permission> findUserPermissions(@Param("userId") UUID userId,
                                        @Param("tenantId") UUID tenantId);

    // Role permissions with hierarchy
    @Query("SELECT DISTINCT p FROM Permission p " +
            "JOIN RolePermission rp ON rp.permission = p " +
            "WHERE rp.role.id = :roleId AND p.isActive = true")
    List<Permission> findByRoleId(@Param("roleId") UUID roleId);

    // Distinct values for dropdowns
    @Query("SELECT DISTINCT p.resourceType FROM Permission p ORDER BY p.resourceType")
    List<String> findDistinctResourceTypes();

    @Query("SELECT DISTINCT p.action FROM Permission p ORDER BY p.action")
    List<String> findDistinctActions();

    // Resource type permissions
    @Query("SELECT p FROM Permission p WHERE p.resourceType IN :resourceTypes")
    List<Permission> findByResourceTypes(@Param("resourceTypes") List<String> resourceTypes);

    // Count queries
    long countByResourceType(String resourceType);

    long countByIsSystemTrue();

    long countByIsActiveTrue();
}