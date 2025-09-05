package com.nnipa.authz.repository;

import com.nnipa.authz.entity.RolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for RolePermission entity operations.
 * Manages the many-to-many relationship between roles and permissions with additional attributes.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    /**
     * Find all role-permission mappings for a specific role.
     */
    List<RolePermission> findByRoleId(UUID roleId);

    /**
     * Find all role-permission mappings for a specific role with pagination.
     */
    Page<RolePermission> findByRoleId(UUID roleId, Pageable pageable);

    /**
     * Find all role-permission mappings for a specific permission.
     */
    List<RolePermission> findByPermissionId(UUID permissionId);

    /**
     * Find a specific role-permission mapping.
     */
    Optional<RolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    /**
     * Delete a specific role-permission mapping.
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    void deleteByRoleIdAndPermissionId(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    /**
     * Delete all permissions for a role.
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteAllByRoleId(@Param("roleId") UUID roleId);

    /**
     * Delete all role assignments for a permission.
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.permission.id = :permissionId")
    void deleteAllByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * Count the number of permissions assigned to a role.
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.role.id = :roleId")
    long countByRoleId(@Param("roleId") UUID roleId);

    /**
     * Count the number of roles that have a specific permission.
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.permission.id = :permissionId")
    long countByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * Check if a role has a specific permission.
     */
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    /**
     * Find all role-permission mappings that are expiring soon.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.expiresAt IS NOT NULL " +
            "AND rp.expiresAt BETWEEN :now AND :expirationThreshold")
    List<RolePermission> findExpiringPermissions(
            @Param("now") Instant now,
            @Param("expirationThreshold") Instant expirationThreshold
    );

    /**
     * Find all expired role-permission mappings.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.expiresAt IS NOT NULL AND rp.expiresAt < :now")
    List<RolePermission> findExpiredPermissions(@Param("now") Instant now);

    /**
     * Remove expired role-permission mappings.
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.expiresAt IS NOT NULL AND rp.expiresAt < :now")
    int deleteExpiredPermissions(@Param("now") Instant now);

    /**
     * Find role-permission mappings with specific constraints.
     */
    @Query(value = "SELECT * FROM role_permissions rp WHERE rp.role_id = :roleId " +
            "AND rp.constraints IS NOT NULL AND rp.constraints != '{}'::jsonb",
            nativeQuery = true)
    List<RolePermission> findByRoleIdWithConstraints(@Param("roleId") UUID roleId);

    /**
     * Find role-permission mappings granted by a specific user.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.grantedBy = :grantedBy")
    List<RolePermission> findByGrantedBy(@Param("grantedBy") String grantedBy);

    /**
     * Find role-permission mappings granted within a time range.
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.grantedAt BETWEEN :startTime AND :endTime")
    List<RolePermission> findByGrantedAtBetween(
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime
    );

    /**
     * Find role-permission mappings for active roles in a tenant.
     */
    @Query("SELECT rp FROM RolePermission rp " +
            "JOIN rp.role r " +
            "WHERE r.tenantId = :tenantId AND r.isActive = true")
    List<RolePermission> findByTenantIdAndActiveRoles(@Param("tenantId") UUID tenantId);

    /**
     * Find role-permission mappings for system roles.
     */
    @Query("SELECT rp FROM RolePermission rp " +
            "JOIN rp.role r " +
            "WHERE r.isSystem = true")
    List<RolePermission> findSystemRolePermissions();

    /**
     * Bulk insert role-permission mappings.
     * Note: For bulk operations, you might want to use JdbcTemplate or batch processing
     * for better performance with large datasets.
     */
    @Modifying
    @Query(value = "INSERT INTO role_permissions (id, role_id, permission_id, constraints, granted_by, granted_at, expires_at) " +
            "VALUES (:id, :roleId, :permissionId, CAST(:constraints AS jsonb), :grantedBy, :grantedAt, :expiresAt)",
            nativeQuery = true)
    void insertRolePermission(
            @Param("id") UUID id,
            @Param("roleId") UUID roleId,
            @Param("permissionId") UUID permissionId,
            @Param("constraints") String constraints,
            @Param("grantedBy") String grantedBy,
            @Param("grantedAt") Instant grantedAt,
            @Param("expiresAt") Instant expiresAt
    );

    /**
     * Update constraints for a role-permission mapping.
     */
    @Modifying
    @Query("UPDATE RolePermission rp SET rp.constraints = :constraints " +
            "WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    void updateConstraints(
            @Param("roleId") UUID roleId,
            @Param("permissionId") UUID permissionId,
            @Param("constraints") Map<String, Object> constraints
    );

    /**
     * Update expiration time for a role-permission mapping.
     */
    @Modifying
    @Query("UPDATE RolePermission rp SET rp.expiresAt = :expiresAt " +
            "WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    void updateExpiresAt(
            @Param("roleId") UUID roleId,
            @Param("permissionId") UUID permissionId,
            @Param("expiresAt") Instant expiresAt
    );

    /**
     * Find permissions for a role hierarchy (including parent roles).
     */
    @Query(value = "WITH RECURSIVE role_hierarchy AS ( " +
            "  SELECT id, parent_role_id FROM roles WHERE id = :roleId " +
            "  UNION ALL " +
            "  SELECT r.id, r.parent_role_id FROM roles r " +
            "  INNER JOIN role_hierarchy rh ON r.id = rh.parent_role_id " +
            ") " +
            "SELECT rp.* FROM role_permissions rp " +
            "WHERE rp.role_id IN (SELECT id FROM role_hierarchy)",
            nativeQuery = true)
    List<RolePermission> findPermissionsWithHierarchy(@Param("roleId") UUID roleId);

    /**
     * Get permission summary for a role.
     */
    @Query("SELECT NEW map(" +
            "  COUNT(rp) as totalPermissions, " +
            "  COUNT(CASE WHEN p.requiresMfa = true THEN 1 END) as mfaRequiredCount, " +
            "  COUNT(CASE WHEN p.requiresApproval = true THEN 1 END) as approvalRequiredCount, " +
            "  COUNT(CASE WHEN rp.expiresAt IS NOT NULL THEN 1 END) as temporaryPermissions) " +
            "FROM RolePermission rp " +
            "JOIN rp.permission p " +
            "WHERE rp.role.id = :roleId")
    Map<String, Long> getPermissionSummaryForRole(@Param("roleId") UUID roleId);
}