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
 * Creates comprehensive role structure for NNIPA statistical platform.
 *
 * Based on 5 core business modules:
 * 1. Data Management
 * 2. Statistical Computing
 * 3. Privacy & Compliance
 * 4. Data Exchange & Collaboration
 * 5. Visualization & Analytics
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantSyncService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    /**
     * Role definition structure for NNIPA platform
     */
    private static class RoleDefinition {
        final String name;
        final String description;
        final int priority;
        final List<String> resourceTypes;

        RoleDefinition(String name, String description, int priority, List<String> resourceTypes) {
            this.name = name;
            this.description = description;
            this.priority = priority;
            this.resourceTypes = resourceTypes;
        }
    }

    /**
     * Comprehensive role definitions for NNIPA platform.
     * Organized by functional domain and priority level.
     */
    private static final RoleDefinition[] DEFAULT_ROLES = {
            // Administrative Roles (Priority 1000+)
            new RoleDefinition("TENANT_ADMIN",
                    "Administrator role with full tenant management capabilities", 1000,
                    List.of("TENANT", "USER", "ROLE", "PERMISSION", "WORKSPACE", "AUDIT", "SYSTEM_CONFIG", "BILLING")),

            // Data Management Roles (Priority 800-900)
            new RoleDefinition("DATA_STEWARD",
                    "Manages data catalogs, quality, lineage, and data lifecycle operations", 900,
                    List.of("DATASET", "DATA_CATALOG", "DATA_QUALITY", "DATA_LINEAGE", "METADATA", "DATA_INGESTION", "DATA_TRANSFORMATION")),

            new RoleDefinition("DATA_CONTRIBUTOR",
                    "Can ingest, upload, and contribute data to the platform", 800,
                    List.of("DATA_INGESTION", "DATASET", "METADATA")),

            // Statistical Computing Roles (Priority 600-700)
            new RoleDefinition("STATISTICIAN",
                    "Full access to statistical computing engine and advanced analytics", 700,
                    List.of("STATISTICAL_ENGINE", "ML_PIPELINE", "ANALYSIS_TEMPLATE", "REPORT", "DATASET", "CUSTOM_METHODOLOGY")),

            new RoleDefinition("DATA_SCIENTIST",
                    "Machine learning pipeline access and model development capabilities", 650,
                    List.of("ML_PIPELINE", "STATISTICAL_ENGINE", "ANALYSIS_TEMPLATE", "DATASET", "MODEL_DEPLOYMENT")),

            new RoleDefinition("ANALYST",
                    "Can run pre-built analyses and standard statistical operations", 600,
                    List.of("ANALYSIS_TEMPLATE", "REPORT", "DATASET", "BASIC_STATISTICS")),

            // Privacy & Compliance Roles (Priority 800-850)
            new RoleDefinition("PRIVACY_OFFICER",
                    "Handles compliance, audit trails, privacy controls, and regulatory requirements", 850,
                    List.of("PRIVACY_SETTINGS", "AUDIT", "COMPLIANCE", "PII_MANAGEMENT", "ENCRYPTION", "DIFFERENTIAL_PRIVACY", "DISCLOSURE_RISK")),

            // Collaboration & Data Exchange Roles (Priority 500-550)
            new RoleDefinition("WORKSPACE_ADMIN",
                    "Manages shared workspaces and collaboration settings", 550,
                    List.of("WORKSPACE", "COLLABORATION", "DATA_SHARING_AGREEMENT", "WORKFLOW_APPROVAL")),

            new RoleDefinition("EXTERNAL_COLLABORATOR",
                    "Limited access for inter-organizational collaboration", 500,
                    List.of("SHARED_WORKSPACE", "COLLABORATIVE_ANALYSIS", "SHARED_DATASET")),

            // Visualization & Analytics Roles (Priority 400-450)
            new RoleDefinition("DASHBOARD_CREATOR",
                    "Can create and manage interactive dashboards and visualizations", 450,
                    List.of("DASHBOARD", "VISUALIZATION", "CHART_LIBRARY", "EXPORT")),

            // General Access Roles (Priority 100-300)
            new RoleDefinition("DATA_CONSUMER",
                    "Read-only access to datasets and published analyses", 300,
                    List.of("DATASET", "REPORT", "PUBLISHED_ANALYSIS")),

            new RoleDefinition("REVIEWER",
                    "Can review and approve statistical outputs before publication", 250,
                    List.of("REPORT", "ANALYSIS_REVIEW", "PUBLICATION_APPROVAL")),

            new RoleDefinition("VIEWER",
                    "Read-only access to dashboards and visualizations", 100,
                    List.of("DASHBOARD", "VISUALIZATION", "PUBLIC_REPORT"))
    };

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

            int createdRoles = 0;
            int assignedPermissions = 0;

            for (RoleDefinition roleDef : DEFAULT_ROLES) {
                // Create role
                Role role = Role.builder()
                        .tenantId(tenantId)
                        .name(roleDef.name)
                        .description(roleDef.description)
                        .priority(roleDef.priority)
                        .isSystem(true)
                        .isActive(true)
                        .createdBy("SYSTEM")
                        .build();

                role = roleRepository.save(role);
                createdRoles++;

                log.debug("Created {} role: {} [correlationId: {}]",
                        roleDef.name, role.getId(), correlationId);

                // Assign permissions to role
                int rolePermissions = assignPermissionsToRole(role, roleDef.resourceTypes, correlationId);
                assignedPermissions += rolePermissions;
            }

            log.info("Created {} default roles with {} total permissions for tenant: {} [correlationId: {}]",
                    createdRoles, assignedPermissions, tenantId, correlationId);

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
     * Assign permissions to a role based on resource types and role hierarchy.
     */
    private int assignPermissionsToRole(Role role, List<String> resourceTypes, String correlationId) {
        log.debug("Assigning permissions to role: {} for resources: {} [correlationId: {}]",
                role.getName(), resourceTypes, correlationId);

        // Get permissions based on role type and resource access
        List<Permission> permissions = getPermissionsForRole(role.getName(), resourceTypes);

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

        log.debug("Assigned {} permissions to role: {} [correlationId: {}]",
                permissionCount, role.getName(), correlationId);

        return permissionCount;
    }

    /**
     * Get appropriate permissions based on role name and resource types.
     */
    private List<Permission> getPermissionsForRole(String roleName, List<String> resourceTypes) {
        List<Permission> allPermissions = permissionRepository.findByResourceTypes(resourceTypes);

        return switch (roleName) {
            // Admin roles get full permissions
            case "TENANT_ADMIN" -> allPermissions;

            // Data management roles
            case "DATA_STEWARD" -> allPermissions.stream()
                    .filter(p -> !p.getAction().equals("DELETE_TENANT"))
                    .toList();
            case "DATA_CONTRIBUTOR" -> allPermissions.stream()
                    .filter(p -> List.of("CREATE", "UPDATE", "READ", "UPLOAD").contains(p.getAction()))
                    .toList();

            // Statistical roles
            case "STATISTICIAN" -> allPermissions.stream()
                    .filter(p -> !p.getAction().startsWith("ADMIN_") && !p.getAction().equals("DELETE_TENANT"))
                    .toList();
            case "DATA_SCIENTIST" -> allPermissions.stream()
                    .filter(p -> List.of("CREATE", "UPDATE", "READ", "EXECUTE", "DEPLOY").contains(p.getAction()))
                    .toList();
            case "ANALYST" -> allPermissions.stream()
                    .filter(p -> List.of("READ", "EXECUTE", "CREATE_REPORT").contains(p.getAction()))
                    .toList();

            // Privacy and compliance
            case "PRIVACY_OFFICER" -> allPermissions.stream()
                    .filter(p -> !p.getAction().equals("DELETE_TENANT"))
                    .toList();

            // Collaboration roles
            case "WORKSPACE_ADMIN" -> allPermissions.stream()
                    .filter(p -> !p.getAction().startsWith("SYSTEM_"))
                    .toList();
            case "EXTERNAL_COLLABORATOR" -> allPermissions.stream()
                    .filter(p -> List.of("READ", "COLLABORATE", "COMMENT").contains(p.getAction()))
                    .toList();

            // Visualization roles
            case "DASHBOARD_CREATOR" -> allPermissions.stream()
                    .filter(p -> List.of("CREATE", "UPDATE", "READ", "PUBLISH", "EXPORT").contains(p.getAction()))
                    .toList();

            // General access roles
            case "DATA_CONSUMER" -> allPermissions.stream()
                    .filter(p -> List.of("READ", "VIEW").contains(p.getAction()))
                    .toList();
            case "REVIEWER" -> allPermissions.stream()
                    .filter(p -> List.of("READ", "REVIEW", "APPROVE", "REJECT").contains(p.getAction()))
                    .toList();
            case "VIEWER" -> allPermissions.stream()
                    .filter(p -> List.of("READ", "VIEW").contains(p.getAction()))
                    .toList();

            default -> List.of();
        };
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
}