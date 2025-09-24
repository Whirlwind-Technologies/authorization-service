package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.AssignRoleRequest;
import com.nnipa.authz.dto.response.RoleResponse;
import com.nnipa.authz.dto.response.UserRoleResponse;
import com.nnipa.authz.entity.Role;
import com.nnipa.authz.entity.UserRole;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.exception.DuplicateResourceException;
import com.nnipa.authz.exception.ResourceNotFoundException;
import com.nnipa.authz.mapper.RoleMapper;
import com.nnipa.authz.repository.RoleRepository;
import com.nnipa.authz.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user role assignments.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
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
                request.getUserId(), request.getRoleId(), request.getTenantId(),
                request.getRoleName(), request.getAssignedBy()
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
     * Returns List<RoleResponse> not List<UserRole>
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user-roles", key = "#userId + ':' + #tenantId")
    public List<RoleResponse> getUserRoles(UUID userId, UUID tenantId) {
        log.debug("Getting roles for user {} in tenant {}", userId, tenantId);

        List<UserRole> userRoles = userRoleRepository.findActiveByUserIdAndTenantId(userId, tenantId);

        return userRoles.stream()
                .filter(UserRole::isActive)
                .map(UserRole::getRole)
                .filter(role -> role != null && role.isActive())
                .map(roleMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get users assigned to a specific role.
     */
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUsersWithRole(UUID roleId, UUID tenantId) {
        log.debug("Getting users with role {} in tenant {}", roleId, tenantId);

        List<UserRole> userRoles = userRoleRepository.findActiveByRoleIdAndTenantId(roleId, tenantId);

        return userRoles.stream()
                .filter(UserRole::isActive)
                .map(this::mapToUserRoleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get user roles across all tenants.
     */
    @Transactional(readOnly = true)
    public List<UserRoleResponse> getUserRolesAllTenants(UUID userId) {
        log.debug("Getting all roles for user {} across all tenants", userId);

        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);

        return userRoles.stream()
                .filter(UserRole::isActive)
                .map(this::mapToUserRoleResponse)
                .collect(Collectors.toList());
    }

    /**
     * Check if user has a specific role.
     */
    @Transactional(readOnly = true)
    public boolean userHasRole(UUID userId, UUID roleId, UUID tenantId) {
        return userRoleRepository.existsByUserIdAndRoleIdAndTenantId(userId, roleId, tenantId);
    }

    /**
     * Get expired user role assignments for cleanup.
     */
    @Transactional(readOnly = true)
    public List<UserRole> getExpiredAssignments() {
        return userRoleRepository.findExpiredAssignments(Instant.now());
    }

    /**
     * Remove expired role assignments.
     */
    @Transactional
    @CacheEvict(value = {"user-permissions", "user-roles"}, allEntries = true)
    public int removeExpiredAssignments() {
        log.info("Removing expired user role assignments");

        List<UserRole> expiredAssignments = getExpiredAssignments();

        for (UserRole assignment : expiredAssignments) {
            assignment.setActive(false);
            userRoleRepository.save(assignment);
        }

        log.info("Deactivated {} expired role assignments", expiredAssignments.size());
        return expiredAssignments.size();
    }

    /**
     * Helper method to map UserRole to UserRoleResponse
     */
    private UserRoleResponse mapToUserRoleResponse(UserRole userRole) {
        return UserRoleResponse.builder()
                .id(userRole.getId())
                .userId(userRole.getUserId())
                .userName(null) // You'd need to fetch from User Service
                .roleId(userRole.getRole().getId())
                .roleName(userRole.getRole().getName())
                .tenantId(userRole.getTenantId())
                .tenantName(null) // You'd need to fetch from Tenant Service
                .assignedBy(userRole.getAssignedBy())
                .assignedAt(userRole.getAssignedAt())
                .expiresAt(userRole.getExpiresAt())
                .isActive(userRole.isActive())
                .build();
    }
}