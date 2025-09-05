package com.nnipa.authz.repository;

import com.nnipa.authz.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Role entity operations.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByNameAndTenantId(String name, UUID tenantId);

    List<Role> findByTenantIdAndIsActiveTrue(UUID tenantId);

    List<Role> findByParentRoleId(UUID parentRoleId);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") UUID id);

    @Query("SELECT r FROM Role r WHERE r.isSystem = true AND r.isActive = true")
    List<Role> findSystemRoles();

    @Query("SELECT r FROM Role r " +
            "LEFT JOIN FETCH r.permissions rp " +
            "LEFT JOIN FETCH rp.permission " +
            "WHERE r.name = :name AND r.tenantId = :tenantId")
    Optional<Role> findByNameAndTenantIdWithPermissions(
            @Param("name") String name,
            @Param("tenantId") UUID tenantId
    );

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.role.id = :roleId AND ur.isActive = true")
    long countActiveUsersByRoleId(@Param("roleId") UUID roleId);

    boolean existsByNameAndTenantId(String name, UUID tenantId);

    /**
     * Find all roles for a tenant with pagination.
     */
    Page<Role> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Search roles by tenant and name containing search term.
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId " +
            "AND LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Role> findByTenantIdAndNameContaining(
            @Param("tenantId") UUID tenantId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Find roles by tenant with specific status.
     */
    Page<Role> findByTenantIdAndIsActive(UUID tenantId, boolean isActive, Pageable pageable);

    /**
     * Count roles by tenant.
     */
    long countByTenantId(UUID tenantId);

    /**
     * Find system roles for a specific tenant (tenant-specific system roles).
     */
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId AND r.isSystem = true")
    List<Role> findSystemRolesByTenant(@Param("tenantId") UUID tenantId);
}