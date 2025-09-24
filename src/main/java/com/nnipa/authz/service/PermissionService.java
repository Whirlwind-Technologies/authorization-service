package com.nnipa.authz.service;

import com.nnipa.authz.dto.response.PermissionResponse;
import com.nnipa.authz.entity.Permission;
import com.nnipa.authz.entity.Role;
import com.nnipa.authz.entity.RolePermission;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.exception.ResourceNotFoundException;
import com.nnipa.authz.mapper.PermissionMapper;
import com.nnipa.authz.repository.PermissionRepository;
import com.nnipa.authz.repository.RolePermissionRepository;
import com.nnipa.authz.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing permissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionMapper permissionMapper;
    private final AuthorizationEventPublisher eventPublisher;

    /**
     * List permissions with optional filtering.
     */
    @Transactional(readOnly = true)
    public Page<PermissionResponse> listPermissions(String resourceType, Boolean isSystem, Pageable pageable) {
        log.debug("Listing permissions with filters - resourceType: {}, isSystem: {}", resourceType, isSystem);

        Page<Permission> permissions;
        if (resourceType != null && isSystem != null) {
            permissions = permissionRepository.findByResourceTypeAndIsActive(resourceType, isSystem, pageable);
        } else if (resourceType != null) {
            permissions = permissionRepository.findByResourceTypeContainingIgnoreCase(resourceType, pageable);
        } else if (isSystem != null) {
            permissions = permissionRepository.findByIsSystem(isSystem, pageable);
        } else {
            permissions = permissionRepository.findAll(pageable);
        }

        return permissions.map(permissionMapper::toResponse);
    }

    /**
     * Get permission by ID.
     */
    @Transactional(readOnly = true)
    public PermissionResponse getPermission(UUID permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));
        return permissionMapper.toResponse(permission);
    }

    /**
     * Get distinct resource types.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "resource-types")
    public List<String> getDistinctResourceTypes() {
        return permissionRepository.findDistinctResourceTypes();
    }

    /**
     * Get distinct actions.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "permission-actions")
    public List<String> getDistinctActions() {
        return permissionRepository.findDistinctActions();
    }

    /**
     * Assign permissions to a role.
     */
    @Transactional
    public void assignPermissionsToRole(UUID roleId, List<UUID> permissionIds) {
        log.info("Assigning {} permissions to role: {}", permissionIds.size(), roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        for (UUID permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));

            // Check if already assigned
            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
                RolePermission rolePermission = RolePermission.builder()
                        .role(role)
                        .permission(permission)
                        .grantedBy("SYSTEM")
                        .grantedAt(Instant.now())
                        .build();
                rolePermissionRepository.save(rolePermission);

                eventPublisher.publishPermissionGrantedEvent(
                        null, role.getTenantId(), roleId, permissionId,
                        permission.getResourceType(), permission.getResourceType(),
                        permission.getAction(), "SYSTEM"
                );
            }
        }
    }

    /**
     * Remove permission from role.
     */
    @Transactional
    public void removePermissionFromRole(UUID roleId, UUID permissionId) {
        log.info("Removing permission {} from role {}", permissionId, roleId);

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);

        eventPublisher.publishPermissionRevokedEvent(roleId, permissionId, "SYSTEM");
    }

    /**
     * Get permissions for a role.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(UUID roleId, boolean includeInherited) {
        log.debug("Getting permissions for role: {}, includeInherited: {}", roleId, includeInherited);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId));

        Set<Permission> permissions = new HashSet<>();

        // Direct permissions
        permissions.addAll(permissionRepository.findByRoleId(roleId));

        // Inherited permissions
        if (includeInherited && role.getParentRole() != null) {
            permissions.addAll(collectInheritedPermissions(role.getParentRole()));
        }

        return permissions.stream()
                .map(permissionMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all permissions for a user in a tenant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user-permissions", key = "#userId + ':' + #tenantId")
    public List<PermissionResponse> getUserPermissions(UUID userId, UUID tenantId) {
        log.debug("Getting permissions for user {} in tenant {}", userId, tenantId);

        Set<Permission> permissions = permissionRepository.findUserPermissions(userId, tenantId);

        return permissions.stream()
                .map(permissionMapper::toResponse)
                .collect(Collectors.toList());
    }

    private Set<Permission> collectInheritedPermissions(Role role) {
        Set<Permission> permissions = new HashSet<>();
        Set<UUID> visitedRoles = new HashSet<>();

        collectPermissionsRecursively(role, permissions, visitedRoles);

        return permissions;
    }

    private void collectPermissionsRecursively(Role role, Set<Permission> permissions, Set<UUID> visitedRoles) {
        if (role == null || visitedRoles.contains(role.getId())) {
            return;
        }

        visitedRoles.add(role.getId());
        permissions.addAll(permissionRepository.findByRoleId(role.getId()));

        if (role.getParentRole() != null) {
            collectPermissionsRecursively(role.getParentRole(), permissions, visitedRoles);
        }
    }
}