package com.nnipa.authz.service;

import com.nnipa.authz.dto.request.AuthorizationRequest;
import com.nnipa.authz.dto.response.AuthorizationResponse;
import com.nnipa.authz.entity.Permission;
import com.nnipa.authz.entity.Policy;
import com.nnipa.authz.entity.Resource;
import com.nnipa.authz.entity.UserRole;
import com.nnipa.authz.entity.Role;
import com.nnipa.authz.entity.RolePermission;
import com.nnipa.authz.enums.PolicyEffect;
import com.nnipa.authz.event.AuthorizationEventPublisher;
import com.nnipa.authz.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core authorization service for permission evaluation.
 * This service implements a multi-layered authorization approach:
 * 1. Direct permission check through user roles
 * 2. Resource-based policy evaluation
 * 3. Tenant-level policy evaluation
 * 4. Inherited permissions through role hierarchy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private final PermissionRepository permissionRepository;
    private final PolicyRepository policyRepository;
    private final ResourceRepository resourceRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PolicyEvaluationService policyEvaluationService;
    private final AuthorizationEventPublisher eventPublisher;

    /**
     * Main authorization method - checks if a user has permission to perform an action on a resource.
     * Uses caching for performance optimization.
     *
     * @param request Authorization request containing user, resource, and action details
     * @return Authorization response indicating whether access is allowed or denied
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "user-permissions", key = "#request.userId + ':' + #request.tenantId + ':' + #request.resource + ':' + #request.action")
    public AuthorizationResponse authorize(AuthorizationRequest request) {
        log.debug("Processing authorization request for user: {} on resource: {} with action: {}",
                request.getUserId(), request.getResource(), request.getAction());

        try {
            // First, check if user has any active roles
            List<UserRole> userRoles = userRoleRepository.findActiveRolesWithPermissions(
                    request.getUserId(),
                    request.getTenantId()
            );

            if (userRoles.isEmpty()) {
                log.debug("User {} has no active roles in tenant {}",
                        request.getUserId(), request.getTenantId());
                return AuthorizationResponse.denied("User has no active roles");
            }

            // Step 1: Get all user permissions through their roles
            Set<Permission> userPermissions = collectUserPermissions(userRoles);

            // Step 2: Check for super admin privileges
            if (hasSuperAdminPrivileges(userRoles)) {
                log.info("User {} has super admin privileges", request.getUserId());
                return AuthorizationResponse.allowed(
                        "Super admin access granted",
                        List.of("SUPER_ADMIN")
                );
            }

            // Step 3: Check direct permission match
            boolean hasDirectPermission = checkDirectPermission(
                    userPermissions,
                    request.getResource(),
                    request.getAction()
            );

            if (hasDirectPermission) {
                log.debug("User {} has direct permission for {}:{}",
                        request.getUserId(), request.getResource(), request.getAction());
                return AuthorizationResponse.allowed(
                        "Direct permission granted",
                        extractPermissionNames(userPermissions)
                );
            }

            // Step 4: Check for wildcard permissions (e.g., RESOURCE:*)
            boolean hasWildcardPermission = checkWildcardPermission(
                    userPermissions,
                    request.getResource(),
                    request.getAction()
            );

            if (hasWildcardPermission) {
                log.debug("User {} has wildcard permission for resource: {}",
                        request.getUserId(), request.getResource());
                return AuthorizationResponse.allowed(
                        "Wildcard permission granted",
                        extractPermissionNames(userPermissions)
                );
            }

            // Step 5: Evaluate resource-based policies if resource identifier provided
            if (request.getResourceId() != null && !request.getResourceId().isEmpty()) {
                AuthorizationResponse resourceResponse = evaluateResourceAuthorization(
                        request, userPermissions
                );

                if (resourceResponse != null && resourceResponse.isAllowed()) {
                    return resourceResponse;
                }
            }

            // Step 6: Evaluate tenant-level policies
            PolicyEffect tenantEffect = evaluateTenantPolicies(
                    request.getTenantId(),
                    request,
                    userPermissions
            );

            if (tenantEffect == PolicyEffect.ALLOW) {
                log.debug("Tenant policy allows access for user: {}", request.getUserId());
                return AuthorizationResponse.allowed(
                        "Tenant policy allows access",
                        extractPermissionNames(userPermissions)
                );
            }

            // Step 7: Check for inherited permissions through role hierarchy
            boolean hasInheritedPermission = checkInheritedPermissions(
                    userRoles,
                    request.getResource(),
                    request.getAction()
            );

            if (hasInheritedPermission) {
                log.debug("User {} has inherited permission", request.getUserId());
                return AuthorizationResponse.allowed(
                        "Inherited permission granted",
                        extractPermissionNames(userPermissions)
                );
            }

            // Access denied - no matching permissions or policies
            log.info("Authorization denied for user: {} on resource: {} with action: {}",
                    request.getUserId(), request.getResource(), request.getAction());

            return AuthorizationResponse.denied(
                    String.format("No permission for %s:%s", request.getResource(), request.getAction())
            );

        } catch (Exception e) {
            log.error("Error during authorization check for user: {}", request.getUserId(), e);
            return AuthorizationResponse.denied("Authorization check failed: " + e.getMessage());
        } finally {
            // Always publish authorization event for audit trail
            try {
                eventPublisher.publishAuthorizationEvent(request);
            } catch (Exception e) {
                log.error("Failed to publish authorization event", e);
            }
        }
    }

    /**
     * Get all permissions for a user in a specific tenant.
     * This method is cached for performance.
     *
     * @param userId The user ID
     * @param tenantId The tenant ID
     * @return Set of permissions the user has
     */
    @Cacheable(value = "user-permissions", key = "#userId + ':' + #tenantId")
    public Set<Permission> getUserPermissions(UUID userId, UUID tenantId) {
        log.debug("Fetching permissions for user: {} in tenant: {}", userId, tenantId);

        Set<Permission> permissions = permissionRepository.findUserPermissions(userId, tenantId);

        log.debug("Found {} permissions for user: {}", permissions.size(), userId);
        return permissions;
    }

    /**
     * Check if a user has a specific permission.
     *
     * @param userId The user ID
     * @param tenantId The tenant ID
     * @param resource The resource type
     * @param action The action to perform
     * @return true if user has the permission, false otherwise
     */
    public boolean hasPermission(UUID userId, UUID tenantId, String resource, String action) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .userId(userId)
                .tenantId(tenantId)
                .resource(resource)
                .action(action)
                .build();

        AuthorizationResponse response = authorize(request);
        return response.isAllowed();
    }

    /**
     * Batch authorization check for multiple resources.
     *
     * @param requests List of authorization requests
     * @return Map of request to authorization response
     */
    public Map<AuthorizationRequest, AuthorizationResponse> batchAuthorize(List<AuthorizationRequest> requests) {
        Map<AuthorizationRequest, AuthorizationResponse> results = new HashMap<>();

        for (AuthorizationRequest request : requests) {
            results.put(request, authorize(request));
        }

        return results;
    }

    // ==================== Private Helper Methods ====================

    /**
     * Collect all permissions from user roles.
     */
    private Set<Permission> collectUserPermissions(List<UserRole> userRoles) {
        Set<Permission> permissions = new HashSet<>();

        for (UserRole userRole : userRoles) {
            if (userRole.isActive() && isRoleValid(userRole)) {
                Role role = userRole.getRole();
                if (role != null && role.isActive()) {
                    for (RolePermission rolePermission : role.getPermissions()) {
                        if (isPermissionValid(rolePermission)) {
                            permissions.add(rolePermission.getPermission());
                        }
                    }
                }
            }
        }

        return permissions;
    }

    /**
     * Check if a role assignment is still valid.
     */
    private boolean isRoleValid(UserRole userRole) {
        if (!userRole.isActive()) {
            return false;
        }

        if (userRole.getExpiresAt() != null && userRole.getExpiresAt().isBefore(Instant.now())) {
            log.debug("Role assignment expired for user role: {}", userRole.getId());
            return false;
        }

        return true;
    }

    /**
     * Check if a permission assignment is still valid.
     */
    private boolean isPermissionValid(RolePermission rolePermission) {
        if (rolePermission.getExpiresAt() != null &&
                rolePermission.getExpiresAt().isBefore(Instant.now())) {
            return false;
        }

        Permission permission = rolePermission.getPermission();
        return permission != null && permission.isActive();
    }

    /**
     * Check if user has super admin privileges.
     */
    private boolean hasSuperAdminPrivileges(List<UserRole> userRoles) {
        return userRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getName()) && role.isActive());
    }

    /**
     * Check if user has direct permission for the resource and action.
     */
    private boolean checkDirectPermission(Set<Permission> permissions, String resourceType, String action) {
        return permissions.stream()
                .anyMatch(p -> p.getResourceType().equals(resourceType)
                        && p.getAction().equals(action)
                        && p.isActive());
    }

    /**
     * Check for wildcard permissions (e.g., RESOURCE:MANAGE covers all actions).
     */
    private boolean checkWildcardPermission(Set<Permission> permissions, String resourceType, String action) {
        // Check for MANAGE permission which typically covers all actions
        boolean hasManagePermission = permissions.stream()
                .anyMatch(p -> p.getResourceType().equals(resourceType)
                        && "MANAGE".equals(p.getAction())
                        && p.isActive());

        if (hasManagePermission) {
            return true;
        }

        // Check for wildcard resource permissions (e.g., *:READ for read all)
        return permissions.stream()
                .anyMatch(p -> "*".equals(p.getResourceType())
                        && p.getAction().equals(action)
                        && p.isActive());
    }

    /**
     * Evaluate resource-specific authorization.
     */
    private AuthorizationResponse evaluateResourceAuthorization(
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        Optional<Resource> resourceOpt = resourceRepository.findByIdentifierWithPolicies(
                request.getResourceId()
        );

        if (resourceOpt.isEmpty()) {
            log.debug("Resource not found: {}", request.getResourceId());
            return null;
        }

        Resource resource = resourceOpt.get();

        // Check if user is the resource owner
        if (resource.getOwnerId() != null && resource.getOwnerId().equals(request.getUserId())) {
            log.debug("User {} is the owner of resource {}", request.getUserId(), resource.getId());
            return AuthorizationResponse.allowed(
                    "Resource owner access granted",
                    List.of("OWNER")
            );
        }

        // Check if resource is public and action is read-only
        if (resource.isPublic() && isReadOnlyAction(request.getAction())) {
            log.debug("Public resource access granted for read action");
            return AuthorizationResponse.allowed(
                    "Public resource access granted",
                    List.of("PUBLIC_ACCESS")
            );
        }

        // Evaluate resource policies
        PolicyEffect effect = evaluateResourcePolicies(resource, request, userPermissions);

        if (effect == PolicyEffect.ALLOW) {
            return AuthorizationResponse.allowed(
                    "Resource policy allows access",
                    extractPermissionNames(userPermissions)
            );
        }

        return null;
    }

    /**
     * Check if action is read-only.
     */
    private boolean isReadOnlyAction(String action) {
        return "READ".equals(action) || "VIEW".equals(action) || "LIST".equals(action);
    }

    /**
     * Evaluate resource-based policies.
     */
    private PolicyEffect evaluateResourcePolicies(
            Resource resource,
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        List<Policy> policies = new ArrayList<>(resource.getPolicies());
        policies.sort((a, b) -> b.getPriority().compareTo(a.getPriority()));

        for (Policy policy : policies) {
            if (!policy.isActive()) {
                continue;
            }

            PolicyEffect effect = policyEvaluationService.evaluate(
                    policy, request, userPermissions
            );

            // DENY takes precedence
            if (effect == PolicyEffect.DENY) {
                log.debug("Policy {} denies access", policy.getName());
                return PolicyEffect.DENY;
            }

            if (effect == PolicyEffect.ALLOW) {
                log.debug("Policy {} allows access", policy.getName());
                return PolicyEffect.ALLOW;
            }
        }

        return PolicyEffect.DENY;
    }

    /**
     * Evaluate tenant-level policies.
     */
    private PolicyEffect evaluateTenantPolicies(
            UUID tenantId,
            AuthorizationRequest request,
            Set<Permission> userPermissions) {

        List<Policy> policies = policyRepository.findActivePoliciesForTenant(tenantId);

        for (Policy policy : policies) {
            PolicyEffect effect = policyEvaluationService.evaluate(
                    policy, request, userPermissions
            );

            if (effect == PolicyEffect.DENY) {
                return PolicyEffect.DENY;
            }

            if (effect == PolicyEffect.ALLOW) {
                return PolicyEffect.ALLOW;
            }
        }

        return PolicyEffect.DENY;
    }

    /**
     * Check for inherited permissions through role hierarchy.
     */
    private boolean checkInheritedPermissions(
            List<UserRole> userRoles,
            String resource,
            String action) {

        for (UserRole userRole : userRoles) {
            if (checkRoleHierarchy(userRole.getRole(), resource, action)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Recursively check role hierarchy for permissions.
     */
    private boolean checkRoleHierarchy(Role role, String resource, String action) {
        if (role == null || !role.isActive()) {
            return false;
        }

        // Check current role permissions
        boolean hasPermission = role.getPermissions().stream()
                .filter(this::isPermissionValid)
                .anyMatch(rp -> {
                    Permission perm = rp.getPermission();
                    return perm.getResourceType().equals(resource)
                            && perm.getAction().equals(action)
                            && perm.isActive();
                });

        if (hasPermission) {
            return true;
        }

        // Check parent role recursively
        if (role.getParentRole() != null) {
            return checkRoleHierarchy(role.getParentRole(), resource, action);
        }

        return false;
    }

    /**
     * Extract permission names for response.
     */
    private List<String> extractPermissionNames(Set<Permission> permissions) {
        return permissions.stream()
                .filter(Permission::isActive)
                .map(p -> p.getResourceType() + ":" + p.getAction())
                .sorted()
                .collect(Collectors.toList());
    }
}