-- Insert default system permissions
INSERT INTO permissions (id, resource_type, action, description, is_system, is_active, risk_level, requires_mfa, requires_approval)
VALUES
-- System Management
(gen_random_uuid(), 'SYSTEM', 'MANAGE', 'Full system management', true, true, 'CRITICAL', true, false),
(gen_random_uuid(), 'SYSTEM', 'MONITOR', 'Monitor system health', true, true, 'LOW', false, false),
(gen_random_uuid(), 'SYSTEM', 'CONFIGURE', 'Configure system settings', true, true, 'HIGH', true, false),

-- Tenant Management
(gen_random_uuid(), 'TENANT', 'CREATE', 'Create new tenant', true, true, 'HIGH', false, true),
(gen_random_uuid(), 'TENANT', 'READ', 'View tenant information', true, true, 'LOW', false, false),
(gen_random_uuid(), 'TENANT', 'UPDATE', 'Update tenant settings', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'TENANT', 'DELETE', 'Delete tenant', true, true, 'CRITICAL', true, true),
(gen_random_uuid(), 'TENANT', 'MANAGE', 'Full tenant management', true, true, 'HIGH', true, false),

-- User Management
(gen_random_uuid(), 'USER', 'CREATE', 'Create new user', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'USER', 'READ', 'View user information', true, true, 'LOW', false, false),
(gen_random_uuid(), 'USER', 'UPDATE', 'Update user information', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'USER', 'DELETE', 'Delete user', true, true, 'HIGH', false, true),
(gen_random_uuid(), 'USER', 'MANAGE', 'Full user management', true, true, 'HIGH', false, false),

-- Role Management
(gen_random_uuid(), 'ROLE', 'CREATE', 'Create new role', true, true, 'HIGH', false, false),
(gen_random_uuid(), 'ROLE', 'READ', 'View role information', true, true, 'LOW', false, false),
(gen_random_uuid(), 'ROLE', 'UPDATE', 'Update role', true, true, 'HIGH', false, false),
(gen_random_uuid(), 'ROLE', 'DELETE', 'Delete role', true, true, 'HIGH', false, true),
(gen_random_uuid(), 'ROLE', 'MANAGE', 'Full role management', true, true, 'HIGH', true, false),

-- Permission Management
(gen_random_uuid(), 'PERMISSION', 'GRANT', 'Grant permissions', true, true, 'HIGH', true, false),
(gen_random_uuid(), 'PERMISSION', 'REVOKE', 'Revoke permissions', true, true, 'HIGH', true, false),
(gen_random_uuid(), 'PERMISSION', 'READ', 'View permissions', true, true, 'LOW', false, false),
(gen_random_uuid(), 'PERMISSION', 'MANAGE', 'Full permission management', true, true, 'CRITICAL', true, false),

-- Data Management
(gen_random_uuid(), 'DATASET', 'CREATE', 'Create dataset', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'DATASET', 'READ', 'View dataset', true, true, 'LOW', false, false),
(gen_random_uuid(), 'DATASET', 'UPDATE', 'Update dataset', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'DATASET', 'DELETE', 'Delete dataset', true, true, 'HIGH', false, false),
(gen_random_uuid(), 'DATASET', 'EXPORT', 'Export dataset', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'DATASET', 'SHARE', 'Share dataset', true, true, 'MEDIUM', false, false),

-- Analytics
(gen_random_uuid(), 'ANALYSIS', 'CREATE', 'Create analysis', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'ANALYSIS', 'READ', 'View analysis', true, true, 'LOW', false, false),
(gen_random_uuid(), 'ANALYSIS', 'UPDATE', 'Update analysis', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'ANALYSIS', 'DELETE', 'Delete analysis', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'ANALYSIS', 'EXECUTE', 'Execute analysis', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'ANALYSIS', 'SCHEDULE', 'Schedule analysis', true, true, 'MEDIUM', false, false),

-- Reports
(gen_random_uuid(), 'REPORT', 'CREATE', 'Create report', true, true, 'LOW', false, false),
(gen_random_uuid(), 'REPORT', 'READ', 'View report', true, true, 'LOW', false, false),
(gen_random_uuid(), 'REPORT', 'UPDATE', 'Update report', true, true, 'LOW', false, false),
(gen_random_uuid(), 'REPORT', 'DELETE', 'Delete report', true, true, 'MEDIUM', false, false),
(gen_random_uuid(), 'REPORT', 'EXPORT', 'Export report', true, true, 'LOW', false, false),
(gen_random_uuid(), 'REPORT', 'SHARE', 'Share report', true, true, 'LOW', false, false);

-- Create system super admin role (tenant-independent)
INSERT INTO roles (id, tenant_id, name, description, is_system, is_active, priority, created_by)
VALUES
    (gen_random_uuid(), NULL, 'SUPER_ADMIN', 'System-wide administrator with full access', true, true, 10000, 'SYSTEM');