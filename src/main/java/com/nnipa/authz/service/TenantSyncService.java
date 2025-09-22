package com.nnipa.authz.service;

import com.nnipa.authz.entity.Role;
import com.nnipa.authz.entity.Permission;
import com.nnipa.authz.entity.RolePermission;
import com.nnipa.authz.repository.RoleRepository;
import com.nnipa.authz.repository.PermissionRepository;
import com.nnipa.authz.repository.RolePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for synchronizing tenant-related authorization data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSyncService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Create default roles for a new tenant.
     *
     * @param tenantId The UUID of the newly created tenant
     * @param correlationId The correlation ID for request tracing
     */
    @Transactional
    public void createDefaultRolesForTenant(UUID tenantId, String correlationId) {
        log.info("Creating default roles for tenant: {} [correlationId: {}]", tenantId, correlationId);

        try {
            // Add correlation ID to MDC for all subsequent log statements in this thread
            org.slf4j.MDC.put("correlationId", correlationId);

            // Create Tenant Admin role
            Role tenantAdmin = Role.builder()
                    .tenantId(tenantId)
                    .name("TENANT_ADMIN")
                    .description("Administrator role for the tenant")
                    .priority(1000)
                    .isSystem(true)
                    .isActive(true)
                    .createdBy("SYSTEM")
                    .build();
            tenantAdmin = roleRepository.save(tenantAdmin);
            log.debug("Created TENANT_ADMIN role: {} [correlationId: {}]", tenantAdmin.getId(), correlationId);

            // Create User role
            Role userRole = Role.builder()
                    .tenantId(tenantId)
                    .name("USER")
                    .description("Standard user role")
                    .priority(100)
                    .isSystem(true)
                    .isActive(true)
                    .createdBy("SYSTEM")
                    .build();
            userRole = roleRepository.save(userRole);
            log.debug("Created USER role: {} [correlationId: {}]", userRole.getId(), correlationId);

            // Create Viewer role
            Role viewerRole = Role.builder()
                    .tenantId(tenantId)
                    .name("VIEWER")
                    .description("Read-only access role")
                    .priority(50)
                    .isSystem(true)
                    .isActive(true)
                    .createdBy("SYSTEM")
                    .build();
            viewerRole = roleRepository.save(viewerRole);
            log.debug("Created VIEWER role: {} [correlationId: {}]", viewerRole.getId(), correlationId);

            // Assign default permissions to roles
            assignDefaultPermissions(tenantAdmin, "ADMIN", correlationId);
            assignDefaultPermissions(userRole, "USER", correlationId);
            assignDefaultPermissions(viewerRole, "VIEWER", correlationId);

            log.info("Default roles created for tenant: {} [correlationId: {}]", tenantId, correlationId);

        } catch (Exception e) {
            log.error("Failed to create default roles for tenant: {} [correlationId: {}]",
                    tenantId, correlationId, e);
            throw new RuntimeException("Failed to create default roles for tenant: " + tenantId, e);
        } finally {
            // Clean up MDC
            org.slf4j.MDC.remove("correlationId");
        }
    }

    /**
     * Assign default permissions to a role based on type.
     */
    private void assignDefaultPermissions(Role role, String roleType, String correlationId) {
        log.debug("Assigning {} permissions to role: {} [correlationId: {}]",
                roleType, role.getId(), correlationId);

        List<Permission> permissions = switch (roleType) {
            case "ADMIN" -> permissionRepository.findByResourceTypes(
                    List.of("TENANT", "USER", "ROLE", "PERMISSION", "DATASET", "ANALYSIS", "REPORT")
            );
            case "USER" -> permissionRepository.findByResourceTypes(
                            List.of("DATASET", "ANALYSIS", "REPORT")
                    ).stream()
                    .filter(p -> !p.getAction().equals("DELETE") && !p.getAction().equals("MANAGE"))
                    .toList();
            case "VIEWER" -> permissionRepository.findByResourceTypes(
                            List.of("DATASET", "ANALYSIS", "REPORT")
                    ).stream()
                    .filter(p -> p.getAction().equals("READ") || p.getAction().equals("VIEW"))
                    .toList();
            default -> List.of();
        };

        int permissionCount = 0;
        for (Permission permission : permissions) {
            RolePermission rolePermission = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .grantedBy("SYSTEM")
                    .grantedAt(Instant.now())
                    .build();
            rolePermissionRepository.save(rolePermission);
            permissionCount++;
        }

        log.debug("Assigned {} permissions to {} role: {} [correlationId: {}]",
                permissionCount, roleType, role.getId(), correlationId);
    }

    /**
     * Deactivate all authorization data for a tenant.
     *
     * @param tenantId The UUID of the tenant to deactivate
     * @param correlationId The correlation ID for request tracing
     */
    @Transactional
    public void deactivateTenantAuthorization(UUID tenantId, String correlationId) {
        log.info("Deactivating authorization for tenant: {} [correlationId: {}]", tenantId, correlationId);

        try {
            // Add correlation ID to MDC for all subsequent log statements in this thread
            org.slf4j.MDC.put("correlationId", correlationId);

            // Deactivate all roles for the tenant
            List<Role> roles = roleRepository.findByTenantIdAndIsActiveTrue(tenantId);
            for (Role role : roles) {
                role.setActive(false);
                roleRepository.save(role);
                log.debug("Deactivated role: {} for tenant: {} [correlationId: {}]",
                        role.getName(), tenantId, correlationId);
            }

            log.info("Deactivated {} roles for tenant: {} [correlationId: {}]",
                    roles.size(), tenantId, correlationId);

        } catch (Exception e) {
            log.error("Failed to deactivate tenant authorization for: {} [correlationId: {}]",
                    tenantId, correlationId, e);
            throw new RuntimeException("Failed to deactivate tenant authorization for: " + tenantId, e);
        } finally {
            // Clean up MDC
            org.slf4j.MDC.remove("correlationId");
        }
    }

    // Overloaded methods to maintain backward compatibility
    public void createDefaultRolesForTenant(UUID tenantId) {
        createDefaultRolesForTenant(tenantId, UUID.randomUUID().toString());
    }

    public void deactivateTenantAuthorization(UUID tenantId) {
        deactivateTenantAuthorization(tenantId, UUID.randomUUID().toString());
    }

    /**
     * Assign default permissions to a role based on type (backward compatibility).
     */
    private void assignDefaultPermissions(Role role, String roleType) {
        assignDefaultPermissions(role, roleType, UUID.randomUUID().toString());
    }
}