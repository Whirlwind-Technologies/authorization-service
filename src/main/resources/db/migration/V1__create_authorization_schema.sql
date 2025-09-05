-- Authorization Service Database Schema
-- Version: 1.0
-- Description: Initial schema for RBAC and policy-based authorization

-- Create roles table
CREATE TABLE IF NOT EXISTS roles (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    parent_role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
    priority INTEGER DEFAULT 100,
    max_users INTEGER,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_role_name_tenant UNIQUE (name, tenant_id)
    );

-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    risk_level VARCHAR(20),
    requires_mfa BOOLEAN DEFAULT FALSE,
    requires_approval BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_permission_resource_action UNIQUE (resource_type, action)
    );

-- Create policies table
CREATE TABLE IF NOT EXISTS policies (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    policy_type VARCHAR(50) NOT NULL,
    effect VARCHAR(10) NOT NULL DEFAULT 'DENY',
    conditions JSONB DEFAULT '{}',
    priority INTEGER DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_policy_name_tenant UNIQUE (name, tenant_id)
    );

-- Create resources table
CREATE TABLE IF NOT EXISTS resources (
                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    resource_identifier VARCHAR(255) NOT NULL UNIQUE,
    resource_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    parent_resource_id UUID REFERENCES resources(id) ON DELETE CASCADE,
    attributes JSONB DEFAULT '{}',
    owner_id UUID,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
    );

-- Create role_permissions join table
CREATE TABLE IF NOT EXISTS role_permissions (
                                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    constraints JSONB DEFAULT '{}',
    granted_by VARCHAR(255),
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id)
    );

-- Create policy_permissions join table
CREATE TABLE IF NOT EXISTS policy_permissions (
                                                  policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (policy_id, permission_id)
    );

-- Create resource_policies join table
CREATE TABLE IF NOT EXISTS resource_policies (
                                                 resource_id UUID NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    policy_id UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,
    PRIMARY KEY (resource_id, policy_id)
    );

-- Create user_roles table
CREATE TABLE IF NOT EXISTS user_roles (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    assigned_by VARCHAR(255),
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_user_role_tenant UNIQUE (user_id, role_id, tenant_id)
    );

-- Create indexes
CREATE INDEX idx_role_tenant ON roles(tenant_id);
CREATE INDEX idx_role_parent ON roles(parent_role_id);
CREATE INDEX idx_role_system ON roles(is_system);
CREATE INDEX idx_permission_system ON permissions(is_system);
CREATE INDEX idx_permission_active ON permissions(is_active);
CREATE INDEX idx_policy_tenant ON policies(tenant_id);
CREATE INDEX idx_policy_type ON policies(policy_type);
CREATE INDEX idx_policy_active ON policies(is_active);
CREATE INDEX idx_resource_tenant ON resources(tenant_id);
CREATE INDEX idx_resource_type ON resources(resource_type);
CREATE INDEX idx_resource_parent ON resources(parent_resource_id);
CREATE INDEX idx_role_permission_role ON role_permissions(role_id);
CREATE INDEX idx_role_permission_permission ON role_permissions(permission_id);
CREATE INDEX idx_user_role_user ON user_roles(user_id);
CREATE INDEX idx_user_role_role ON user_roles(role_id);
CREATE INDEX idx_user_role_tenant ON user_roles(tenant_id);

-- Add comments
COMMENT ON TABLE roles IS 'Hierarchical roles for RBAC';
COMMENT ON TABLE permissions IS 'Granular permissions for resources and actions';
COMMENT ON TABLE policies IS 'Authorization policies for fine-grained access control';
COMMENT ON TABLE resources IS 'Protected resources with hierarchical structure';
COMMENT ON TABLE role_permissions IS 'Role to permission assignments with constraints';
COMMENT ON TABLE user_roles IS 'User to role assignments per tenant';