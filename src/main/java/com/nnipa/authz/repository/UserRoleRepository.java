package com.nnipa.authz.repository;

import com.nnipa.authz.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserRole entity operations.
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    List<UserRole> findByUserIdAndIsActiveTrue(UUID userId);

    Optional<UserRole> findByUserIdAndRoleIdAndTenantId(UUID userId, UUID roleId, UUID tenantId);

    @Query("SELECT ur FROM UserRole ur " +
            "JOIN FETCH ur.role r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE ur.userId = :userId AND ur.tenantId = :tenantId AND ur.isActive = true")
    List<UserRole> findActiveRolesWithPermissions(
            @Param("userId") UUID userId,
            @Param("tenantId") UUID tenantId
    );

    @Modifying
    @Query("UPDATE UserRole ur SET ur.isActive = false " +
            "WHERE ur.expiresAt IS NOT NULL AND ur.expiresAt < :now")
    int deactivateExpiredRoles(@Param("now") Instant now);

    @Query("SELECT COUNT(ur) FROM UserRole ur " +
            "WHERE ur.role.id = :roleId AND ur.isActive = true")
    long countActiveUsersByRoleId(@Param("roleId") UUID roleId);

    boolean existsByUserIdAndRoleIdAndTenantId(UUID userId, UUID roleId, UUID tenantId);
}