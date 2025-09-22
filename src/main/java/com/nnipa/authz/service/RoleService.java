package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.CreateRoleRequest;
import com.nnipa.authz.dto.request.UpdateRoleRequest;
import com.nnipa.authz.dto.request.AssignPermissionsRequest;
import com.nnipa.authz.dto.response.RoleResponse;
import com.nnipa.authz.dto.response.PermissionResponse;
import com.nnipa.authz.dto.response.RoleHierarchyResponse;
import com.nnipa.authz.entity.Role;
import com.nnipa.authz.entity.RolePermission;
import com.nnipa.authz.entity.Permission;
import com.nnipa.authz.entity.UserRole;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.exception.ResourceNotFoundException;
import com.nnipa.authz.exception.DuplicateResourceException;
import com.nnipa.authz.exception.ValidationException;
import com.nnipa.authz.exception.BusinessRuleException;
import com.nnipa.authz.mapper.RoleMapper;
import com.nnipa.authz.mapper.PermissionMapper;
import com.nnipa.authz.repository.RoleRepository;
import com.nnipa.authz.repository.PermissionRepository;
import com.nnipa.authz.repository.RolePermissionRepository;
import com.nnipa.authz.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing roles and role assignments.
 * Handles RBAC operations including role hierarchy, permission management, and tenant isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final AuthorizationEventPublisher eventPublisher;

    @Value("${authz.role.max-hierarchy-depth:10}")
    private int maxHierarchyDepth;

    @Value("${authz.role.max-permissions-per-role:100}")
    private int maxPermissionsPerRole;

    /**
     * Create a new role.
     */
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RoleResponse createRole(CreateRoleRequest request) {
        log.info("Creating role: {} for tenant: {}", request.getName(), request.getTenantId());

        // Validate role doesn't already exist
        if (roleRepository.existsByNameAndTenantId(request.getName(), request.getTenantId())) {
            throw new DuplicateResourceException(
                    String.format("Role with name '%s' already exists for this tenant", request.getName())
            );
        }

        // Create role entity
        Role role = Role.builder()
                .tenantId(request.getTenantId())
                .name(request.getName())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .maxUsers(request.getMaxUsers())
                .isSystem(false)
                .isActive(true)
                .createdBy(request.getCreatedBy())
                .build();

        // Set parent role if specified
        if (request.getParentRoleId() != null) {
            Role parentRole = roleRepository.findById(request.getParentRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent role not found"));

            // Validate parent role is in same tenant
            if (!parentRole.getTenantId().equals(request.getTenantId())) {
                throw new ValidationException("Parent role must be in the same tenant");
            }

            // Check hierarchy depth
            validateHierarchyDepth(parentRole);

            role.setParentRole(parentRole);
        }

        role = roleRepository.save(role);

        // Assign initial permissions if provided
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            assignPermissionsToRole(role, request.getPermissionIds(), request.getCreatedBy());
        }

        // Publish role created event
        eventPublisher.publishRoleCreatedEvent(
                role.getId(),
                role.getTenantId(),
                role.getName(),
                role.getDescription(),
                request.getCreatedBy()
        );

        log.info("Role created successfully: {}", role.getId());
        return roleMapper.toResponse(role);
    }

    /**
     * Update an existing role.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public RoleResponse updateRole(UUID roleId, UpdateRoleRequest request) {
        log.info("Updating role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Track changes for event publishing
        Map<String, String> changes = new HashMap<>();

        // Prevent updating system roles
        if (role.isSystem() && !Boolean.TRUE.equals(request.getAllowSystemUpdate())) {
            throw new BusinessRuleException("System roles cannot be modified");
        }

        // Update basic fields
        if (request.getName() != null && !role.getName().equals(request.getName())) {
            // Check for duplicate name
            if (roleRepository.existsByNameAndTenantId(request.getName(), role.getTenantId())) {
                throw new DuplicateResourceException("Role name already exists");
            }
            changes.put("name", String.format("%s -> %s", role.getName(), request.getName()));
            role.setName(request.getName());
        }

        if (request.getDescription() != null && !Objects.equals(role.getDescription(), request.getDescription())) {
            changes.put("description", String.format("%s -> %s", role.getDescription(), request.getDescription()));
            role.setDescription(request.getDescription());
        }

        if (request.getPriority() != null && !Objects.equals(role.getPriority(), request.getPriority())) {
            changes.put("priority", String.format("%d -> %d", role.getPriority(), request.getPriority()));
            role.setPriority(request.getPriority());
        }

        if (request.getMaxUsers() != null && !Objects.equals(role.getMaxUsers(), request.getMaxUsers())) {
            // Validate max users constraint
            long currentUsers = userRoleRepository.countActiveUsersByRoleId(roleId);
            if (request.getMaxUsers() < currentUsers) {
                throw new BusinessRuleException(
                        String.format("Cannot set max users to %d. Role currently has %d users",
                                request.getMaxUsers(), currentUsers)
                );
            }
            changes.put("maxUsers", String.format("%s -> %d",
                    role.getMaxUsers() != null ? role.getMaxUsers().toString() : "null",
                    request.getMaxUsers()));
            role.setMaxUsers(request.getMaxUsers());
        }

        if (request.getIsActive() != null && !Objects.equals(role.isActive(), request.getIsActive())) {
            changes.put("isActive", String.format("%b -> %b", role.isActive(), request.getIsActive()));
            role.setActive(request.getIsActive());
        }

        role.setUpdatedBy(request.getUpdatedBy());
        role = roleRepository.save(role);

        // Publish role updated event if there were changes
        if (!changes.isEmpty()) {
            eventPublisher.publishRoleUpdatedEvent(roleId, role.getTenantId(), changes, request.getUpdatedBy());
        }

        log.info("Role updated successfully: {}", roleId);
        return roleMapper.toResponse(role);
    }

    /**
     * Get role by ID.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#roleId")
    public RoleResponse getRole(UUID roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        RoleResponse response = roleMapper.toResponse(role);

        // Include current user count
        long userCount = userRoleRepository.countActiveUsersByRoleId(roleId);
        response.setCurrentUsers(userCount);

        return response;
    }

    /**
     * Get role by name and tenant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#name + ':' + #tenantId")
    public RoleResponse getRoleByName(String name, UUID tenantId) {
        Role role = roleRepository.findByNameAndTenantIdWithPermissions(name, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Role '%s' not found for tenant", name)
                ));
        return roleMapper.toResponse(role);
    }

    /**
     * Get all roles for a tenant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#tenantId")
    public List<RoleResponse> getTenantRoles(UUID tenantId) {
        List<Role> roles = roleRepository.findByTenantIdAndIsActiveTrue(tenantId);
        return roles.stream()
                .map(roleMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get system roles.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "'system'")
    public List<RoleResponse> getSystemRoles() {
        List<Role> roles = roleRepository.findSystemRoles();
        return roles.stream()
                .map(roleMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get role hierarchy.
     */
    @Transactional(readOnly = true)
    public RoleHierarchyResponse getRoleHierarchy(UUID roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        return buildHierarchyResponse(role);
    }

    /**
     * Search roles with pagination.
     */
    @Transactional(readOnly = true)
    public Page<RoleResponse> searchRoles(UUID tenantId, String searchTerm, Pageable pageable) {
        Page<Role> roles;

        if (searchTerm != null && !searchTerm.isEmpty()) {
            roles = roleRepository.findByTenantIdAndNameContaining(tenantId, searchTerm, pageable);
        } else {
            roles = roleRepository.findByTenantId(tenantId, pageable);
        }

        return roles.map(roleMapper::toResponse);
    }

    /**
     * Assign permissions to a role.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void assignPermissionsToRole(Role role, Set<UUID> permissionIds, String assignedBy) {
        log.info("Assigning {} permissions to role: {}", permissionIds.size(), role.getName());

        // Validate permission limit
        long existingPermissions = rolePermissionRepository.countByRoleId(role.getId());
        if (existingPermissions + permissionIds.size() > maxPermissionsPerRole) {
            throw new BusinessRuleException(
                    String.format("Role cannot have more than %d permissions", maxPermissionsPerRole)
            );
        }

        List<RolePermission> newPermissions = new ArrayList<>();

        for (UUID permissionId : permissionIds) {
            // Skip if already assigned
            if (rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permissionId)) {
                continue;
            }

            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + permissionId));

            RolePermission rolePermission = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .grantedBy(assignedBy)
                    .grantedAt(Instant.now())
                    .build();

            newPermissions.add(rolePermission);
        }

        if (!newPermissions.isEmpty()) {
            rolePermissionRepository.saveAll(newPermissions);
            log.info("Assigned {} new permissions to role", newPermissions.size());

            // Publish events for each permission - FIXED method signature
            for (RolePermission rp : newPermissions) {
                // Get current user ID and context for the event
                String currentUserId = getCurrentUserId();

                eventPublisher.publishPermissionGrantedEvent(
                        currentUserId != null ? UUID.fromString(currentUserId) : null,  // userId
                        role.getTenantId(),                                             // tenantId
                        role.getId(),                                                   // roleId
                        rp.getPermission().getId(),                                     // permissionId
                        rp.getPermission().getResourceType(),                       // resource
                        rp.getPermission().getResourceType(),                          // resourceType
                        rp.getPermission().getAction(),                                // action
                        assignedBy                                                      // grantedBy
                );
            }
        }
    }

    /**
     * Bulk assign permissions to a role.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void bulkAssignPermissions(UUID roleId, AssignPermissionsRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        assignPermissionsToRole(role, request.getPermissionIds(), request.getAssignedBy());
    }

    /**
     * Remove permission from role.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void removePermissionFromRole(UUID roleId, UUID permissionId) {
        log.info("Removing permission {} from role {}", permissionId, roleId);

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);

        eventPublisher.publishPermissionRevokedEvent(roleId, permissionId, getCurrentUserName());

        log.info("Permission removed from role");
    }

    /**
     * Remove all permissions from a role.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void removeAllPermissions(UUID roleId) {
        log.info("Removing all permissions from role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.isSystem()) {
            throw new BusinessRuleException("Cannot remove permissions from system role");
        }

        rolePermissionRepository.deleteAllByRoleId(roleId);

        log.info("All permissions removed from role");
    }

    /**
     * Get all permissions for a role including inherited from parent roles.
     */
    @Transactional(readOnly = true)
    public Set<PermissionResponse> getAllPermissionsIncludingInherited(UUID roleId) {
        log.debug("Getting all permissions including inherited for role: {}", roleId);

        Set<Permission> allPermissions = new HashSet<>();
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Collect permissions from this role and all parent roles
        collectPermissionsRecursively(role, allPermissions, new HashSet<>());

        return allPermissions.stream()
                .filter(Permission::isActive)
                .map(permissionMapper::toResponse)
                .collect(Collectors.toSet());
    }

    /**
     * Get permissions expiring soon for a role.
     */
    @Transactional(readOnly = true)
    public List<RolePermission> getExpiringPermissions(UUID roleId, int daysAhead) {
        Instant now = Instant.now();
        Instant threshold = now.plus(daysAhead, ChronoUnit.DAYS);

        return rolePermissionRepository.findExpiringPermissions(now, threshold).stream()
                .filter(rp -> rp.getRole().getId().equals(roleId))
                .collect(Collectors.toList());
    }

    /**
     * Set expiration for a role-permission.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void setPermissionExpiration(UUID roleId, UUID permissionId, Instant expiresAt) {
        log.info("Setting expiration for permission {} in role {} to {}",
                permissionId, roleId, expiresAt);

        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw new ValidationException("Expiration time must be in the future");
        }

        rolePermissionRepository.updateExpiresAt(roleId, permissionId, expiresAt);

        log.info("Permission expiration updated");
    }

    /**
     * Update permission constraints for a role.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void updatePermissionConstraints(UUID roleId, UUID permissionId, Map<String, Object> constraints) {
        log.info("Updating constraints for permission {} in role {}", permissionId, roleId);

        RolePermission rolePermission = rolePermissionRepository
                .findByRoleIdAndPermissionId(roleId, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Role-Permission mapping not found"));

        rolePermission.setConstraints(constraints != null ? constraints : new HashMap<>());
        rolePermissionRepository.save(rolePermission);

        log.info("Permission constraints updated");
    }

    /**
     * Clone a role with its permissions.
     */
    @Transactional
    @CacheEvict(value = "roles", allEntries = true)
    public RoleResponse cloneRole(UUID sourceRoleId, String newName, UUID tenantId, String createdBy) {
        log.info("Cloning role {} with new name: {}", sourceRoleId, newName);

        Role sourceRole = roleRepository.findByIdWithPermissions(sourceRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("Source role not found"));

        // Validate new name
        if (roleRepository.existsByNameAndTenantId(newName, tenantId)) {
            throw new DuplicateResourceException("Role with name already exists");
        }

        // Create new role
        Role newRole = Role.builder()
                .tenantId(tenantId)
                .name(newName)
                .description(sourceRole.getDescription() + " (Cloned)")
                .priority(sourceRole.getPriority())
                .maxUsers(sourceRole.getMaxUsers())
                .parentRole(sourceRole.getParentRole())
                .isSystem(false)
                .isActive(true)
                .createdBy(createdBy)
                .build();

        newRole = roleRepository.save(newRole);

        // Clone permissions
        Set<UUID> permissionIds = sourceRole.getPermissions().stream()
                .map(rp -> rp.getPermission().getId())
                .collect(Collectors.toSet());

        if (!permissionIds.isEmpty()) {
            assignPermissionsToRole(newRole, permissionIds, createdBy);
        }

        log.info("Role cloned successfully: {}", newRole.getId());
        return roleMapper.toResponse(newRole);
    }

    /**
     * Delete a role.
     */
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void deleteRole(UUID roleId) {
        log.info("Deleting role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        // Prevent deleting system roles
        if (role.isSystem()) {
            throw new BusinessRuleException("System roles cannot be deleted");
        }

        // Check if role is in use
        long userCount = userRoleRepository.countActiveUsersByRoleId(roleId);
        if (userCount > 0) {
            throw new BusinessRuleException(
                    String.format("Cannot delete role. %d users are assigned to this role", userCount)
            );
        }

        // Check if role has child roles
        List<Role> childRoles = roleRepository.findByParentRoleId(roleId);
        if (!childRoles.isEmpty()) {
            throw new BusinessRuleException(
                    String.format("Cannot delete role. It has %d child roles", childRoles.size())
            );
        }

        // Delete role (cascade will handle permissions)
        roleRepository.deleteById(roleId);

        eventPublisher.publishRoleDeletedEvent(roleId, role.getTenantId(), getCurrentUserName());

        log.info("Role deleted successfully");
    }

    /**
     * Remove expired permissions (scheduled task).
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    @CacheEvict(value = {"roles", "user-permissions"}, allEntries = true)
    public void removeExpiredPermissions() {
        log.debug("Running scheduled task to remove expired permissions");

        int deleted = rolePermissionRepository.deleteExpiredPermissions(Instant.now());

        if (deleted > 0) {
            log.info("Removed {} expired role-permission mappings", deleted);
        }
    }

    /**
     * Get role statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getRoleStatistics(UUID roleId) {
        Map<String, Object> stats = new HashMap<>();

        // Basic counts
        stats.put("totalPermissions", rolePermissionRepository.countByRoleId(roleId));
        stats.put("activeUsers", userRoleRepository.countActiveUsersByRoleId(roleId));

        // Permission summary
        Map<String, Long> permissionSummary = rolePermissionRepository.getPermissionSummaryForRole(roleId);
        stats.putAll(permissionSummary);

        // Child roles count
        List<Role> childRoles = roleRepository.findByParentRoleId(roleId);
        stats.put("childRoles", childRoles.size());

        return stats;
    }

    /**
     * Find roles by tenant with custom filtering.
     */
    @Transactional(readOnly = true)
    public Page<RoleResponse> findRolesByTenant(UUID tenantId, Boolean isActive, Pageable pageable) {
        Page<Role> roles;

        if (isActive != null) {
            roles = roleRepository.findByTenantIdAndIsActive(tenantId, isActive, pageable);
        } else {
            roles = roleRepository.findByTenantId(tenantId, pageable);
        }

        return roles.map(roleMapper::toResponse);
    }

    /**
     * Count roles in a tenant.
     */
    @Transactional(readOnly = true)
    public long countRolesByTenant(UUID tenantId) {
        return roleRepository.countByTenantId(tenantId);
    }

    /**
     * Get system roles for a specific tenant.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "roles", key = "#tenantId + ':system'")
    public List<RoleResponse> getSystemRolesByTenant(UUID tenantId) {
        List<Role> roles = roleRepository.findSystemRolesByTenant(tenantId);
        return roles.stream()
                .map(roleMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    /**
     * Get current user ID from security context.
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * Get current user name from security context.
     */
    private String getCurrentUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    /**
     * Validate hierarchy depth doesn't exceed maximum.
     */
    private void validateHierarchyDepth(Role parentRole) {
        int depth = 0;
        Role current = parentRole;

        while (current != null && depth < maxHierarchyDepth) {
            current = current.getParentRole();
            depth++;
        }

        if (depth >= maxHierarchyDepth) {
            throw new BusinessRuleException(
                    String.format("Role hierarchy cannot exceed %d levels", maxHierarchyDepth)
            );
        }
    }

    /**
     * Recursively collect permissions from role hierarchy.
     */
    private void collectPermissionsRecursively(Role role, Set<Permission> permissions, Set<UUID> visitedRoles) {
        // Avoid circular references
        if (visitedRoles.contains(role.getId())) {
            return;
        }
        visitedRoles.add(role.getId());

        // Add permissions from current role
        for (RolePermission rp : role.getPermissions()) {
            // Check if permission is still valid
            if (rp.getExpiresAt() == null || rp.getExpiresAt().isAfter(Instant.now())) {
                permissions.add(rp.getPermission());
            }
        }

        // Recursively add permissions from parent role
        if (role.getParentRole() != null) {
            collectPermissionsRecursively(role.getParentRole(), permissions, visitedRoles);
        }
    }

    /**
     * Build role hierarchy response.
     */
    private RoleHierarchyResponse buildHierarchyResponse(Role role) {
        RoleHierarchyResponse response = new RoleHierarchyResponse();
        response.setRole(roleMapper.toResponse(role));

        // Add parent hierarchy
        List<RoleResponse> parentHierarchy = new ArrayList<>();
        Role parent = role.getParentRole();
        while (parent != null) {
            parentHierarchy.add(roleMapper.toResponse(parent));
            parent = parent.getParentRole();
        }
        response.setParentHierarchy(parentHierarchy);

        // Add child roles
        List<Role> children = roleRepository.findByParentRoleId(role.getId());
        response.setChildRoles(children.stream()
                .map(roleMapper::toResponse)
                .collect(Collectors.toList()));

        // Add all permissions including inherited
        response.setAllPermissions(new ArrayList<>(getAllPermissionsIncludingInherited(role.getId())));

        return response;
    }
}