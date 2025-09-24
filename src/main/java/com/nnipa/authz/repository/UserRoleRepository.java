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

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    // Existing methods
    boolean existsByUserIdAndRoleIdAndTenantId(UUID userId, UUID roleId, UUID tenantId);

    // Add this missing method
    boolean existsByUserIdAndTenantId(UUID userId, UUID tenantId);

    Optional<UserRole> findByUserIdAndRoleIdAndTenantId(UUID userId, UUID roleId, UUID tenantId);

    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.role.id = :roleId AND ur.isActive = true")
    long countActiveUsersByRoleId(@Param("roleId") UUID roleId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r WHERE ur.userId = :userId " +
            "AND ur.tenantId = :tenantId AND ur.isActive = true")
    List<UserRole> findActiveRolesWithPermissions(@Param("userId") UUID userId,
                                                  @Param("tenantId") UUID tenantId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.userId = :userId " +
            "AND ur.tenantId = :tenantId AND ur.isActive = true")
    List<UserRole> findActiveByUserIdAndTenantId(@Param("userId") UUID userId,
                                                 @Param("tenantId") UUID tenantId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.role.id = :roleId " +
            "AND ur.tenantId = :tenantId AND ur.isActive = true")
    List<UserRole> findActiveByRoleIdAndTenantId(@Param("roleId") UUID roleId,
                                                 @Param("tenantId") UUID tenantId);

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.userId = :userId " +
            "AND ur.isActive = true")
    List<UserRole> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.expiresAt < :now AND ur.isActive = true")
    List<UserRole> findExpiredAssignments(@Param("now") Instant now);

    // Add this for expired permissions cleanup
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.expiresAt < :now")
    int deleteExpiredPermissions(@Param("now") Instant now);
}