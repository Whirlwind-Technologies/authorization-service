package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.AssignRoleRequest;
import com.nnipa.authz.entity.Role;
import com.nnipa.authz.entity.UserRole;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.exception.DuplicateResourceException;
import com.nnipa.authz.exception.ResourceNotFoundException;
import com.nnipa.authz.repository.RoleRepository;
import com.nnipa.authz.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing user-role assignments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AuthorizationEventPublisher eventPublisher;

    /**
     * Assign a role to a user.
     */
    @Transactional
    @CacheEvict(value = "user-permissions", key = "#request.userId + ':' + #request.tenantId", allEntries = true)
    public void assignRoleToUser(AssignRoleRequest request) {
        log.info("Assigning role {} to user {} in tenant {}",
                request.getRoleId(), request.getUserId(), request.getTenantId());

        // Check if assignment already exists
        if (userRoleRepository.existsByUserIdAndRoleIdAndTenantId(
                request.getUserId(), request.getRoleId(), request.getTenantId())) {
            throw new DuplicateResourceException("User already has this role");
        }

        // Verify role exists
        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Check max users limit for role
        if (role.getMaxUsers() != null) {
            long currentUsers = userRoleRepository.countActiveUsersByRoleId(role.getId());
            if (currentUsers >= role.getMaxUsers()) {
                throw new IllegalStateException(
                        String.format("Role has reached maximum user limit of %d", role.getMaxUsers())
                );
            }
        }

        // Create assignment
        UserRole userRole = UserRole.builder()
                .userId(request.getUserId())
                .role(role)
                .tenantId(request.getTenantId())
                .assignedBy(request.getAssignedBy())
                .assignedAt(Instant.now())
                .expiresAt(request.getExpiresAt())
                .isActive(true)
                .build();

        userRoleRepository.save(userRole);

        // Publish event
        eventPublisher.publishRoleAssignedEvent(
                request.getUserId(), request.getRoleId(), request.getTenantId()
        );

        log.info("Role assigned successfully");
    }

    /**
     * Revoke a role from a user.
     */
    @Transactional
    @CacheEvict(value = "user-permissions", key = "#userId + ':' + #tenantId", allEntries = true)
    public void revokeRoleFromUser(UUID userId, UUID roleId, UUID tenantId) {
        log.info("Revoking role {} from user {} in tenant {}", roleId, userId, tenantId);

        UserRole userRole = userRoleRepository.findByUserIdAndRoleIdAndTenantId(userId, roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User role assignment not found"));

        userRole.setActive(false);
        userRoleRepository.save(userRole);

        log.info("Role revoked successfully");
    }

    /**
     * Get all roles for a user in a tenant.
     */
    @Transactional(readOnly = true)
    public List<UserRole> getUserRoles(UUID userId, UUID tenantId) {
        return userRoleRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    /**
     * Deactivate expired role assignments.
     */
    @Transactional
    @CacheEvict(value = "user-permissions", allEntries = true)
    public int deactivateExpiredRoles() {
        int count = userRoleRepository.deactivateExpiredRoles(Instant.now());
        if (count > 0) {
            log.info("Deactivated {} expired role assignments", count);
        }
        return count;
    }
}