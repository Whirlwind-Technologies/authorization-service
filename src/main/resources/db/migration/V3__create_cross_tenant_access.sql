-- Cross-Tenant Access Management
CREATE TABLE IF NOT EXISTS cross_tenant_access (
                                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_tenant_id UUID NOT NULL,
    target_tenant_id UUID NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    conditions JSONB,
    granted_by VARCHAR(255) NOT NULL,
    granted_at TIMESTAMP NOT NULL,
    revoked_by VARCHAR(255),
    revoked_at TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,

    CONSTRAINT uk_cross_tenant_unique UNIQUE (source_tenant_id, target_tenant_id, resource_type, resource_id)
    );

-- Table for storing permissions list (required by @ElementCollection)
CREATE TABLE IF NOT EXISTS cross_tenant_permissions (
                                                        access_id UUID NOT NULL,
                                                        permission VARCHAR(255) NOT NULL,
    CONSTRAINT fk_cross_tenant_permissions_access
    FOREIGN KEY (access_id) REFERENCES cross_tenant_access(id) ON DELETE CASCADE
    );

-- Indexes for performance
CREATE INDEX idx_cross_tenant_source ON cross_tenant_access(source_tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_cross_tenant_target ON cross_tenant_access(target_tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_cross_tenant_resource ON cross_tenant_access(resource_type);
CREATE INDEX idx_cross_tenant_expires ON cross_tenant_access(expires_at) WHERE is_active = TRUE;
CREATE INDEX idx_cross_tenant_permissions_access ON cross_tenant_permissions(access_id);

-- Audit table for cross-tenant access history
CREATE TABLE IF NOT EXISTS cross_tenant_access_audit (
                                                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    access_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL, -- GRANT, REVOKE, EXPIRE, MODIFY
    performed_by VARCHAR(255) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX idx_cross_tenant_audit_access ON cross_tenant_access_audit(access_id);
CREATE INDEX idx_cross_tenant_audit_time ON cross_tenant_access_audit(performed_at);