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
     */
    @Transactional
    public void createDefaultRolesForTenant(UUID tenantId) {
        log.info("Creating default roles for tenant: {}", tenantId);

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

        // Assign default permissions to roles
        assignDefaultPermissions(tenantAdmin, "ADMIN");
        assignDefaultPermissions(userRole, "USER");
        assignDefaultPermissions(viewerRole, "VIEWER");

        log.info("Default roles created for tenant: {}", tenantId);
    }

    /**
     * Assign default permissions to a role based on type.
     */
    private void assignDefaultPermissions(Role role, String roleType) {
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

        for (Permission permission : permissions) {
            RolePermission rolePermission = RolePermission.builder()
                    .role(role)
                    .permission(permission)
                    .grantedBy("SYSTEM")
                    .grantedAt(Instant.now())
                    .build();
            rolePermissionRepository.save(rolePermission);
        }
    }

    /**
     * Deactivate all authorization data for a tenant.
     */
    @Transactional
    public void deactivateTenantAuthorization(UUID tenantId) {
        log.info("Deactivating authorization for tenant: {}", tenantId);

        // Deactivate all roles for the tenant
        List<Role> roles = roleRepository.findByTenantIdAndIsActiveTrue(tenantId);
        for (Role role : roles) {
            role.setActive(false);
            roleRepository.save(role);
        }

        log.info("Deactivated {} roles for tenant: {}", roles.size(), tenantId);
    }
}