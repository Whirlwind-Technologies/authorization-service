-- Insert default system permissions
-- This script creates permissions for ALL resource types used in TenantSyncService
-- Organized by functional domain and synchronized with role definitions

-- System Configuration (renamed from SYSTEM)
INSERT INTO permissions (id, resource_type, action, description, is_system, is_active, risk_level, requires_mfa, requires_approval)
VALUES
    (gen_random_uuid(), 'SYSTEM_CONFIG', 'MANAGE', 'Full system configuration management', true, true, 'CRITICAL', true, false),
    (gen_random_uuid(), 'SYSTEM_CONFIG', 'READ', 'View system configuration', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'SYSTEM_CONFIG', 'UPDATE', 'Update system configuration', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'SYSTEM_CONFIG', 'MONITOR', 'Monitor system health', true, true, 'LOW', false, false),

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

-- Workspace Management
    (gen_random_uuid(), 'WORKSPACE', 'CREATE', 'Create workspace', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'WORKSPACE', 'READ', 'View workspace', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'WORKSPACE', 'UPDATE', 'Update workspace', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'WORKSPACE', 'DELETE', 'Delete workspace', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'WORKSPACE', 'MANAGE', 'Full workspace management', true, true, 'HIGH', false, false),

-- Shared Workspace (for external collaboration)
    (gen_random_uuid(), 'SHARED_WORKSPACE', 'READ', 'View shared workspace', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'SHARED_WORKSPACE', 'COLLABORATE', 'Collaborate in shared workspace', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'SHARED_WORKSPACE', 'COMMENT', 'Add comments to shared workspace', true, true, 'LOW', false, false),

-- Audit Management
    (gen_random_uuid(), 'AUDIT', 'READ', 'View audit logs', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'AUDIT', 'EXPORT', 'Export audit logs', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'AUDIT', 'MANAGE', 'Full audit management', true, true, 'HIGH', true, false),

-- Billing Management
    (gen_random_uuid(), 'BILLING', 'READ', 'View billing information', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'BILLING', 'UPDATE', 'Update billing settings', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'BILLING', 'MANAGE', 'Full billing management', true, true, 'HIGH', true, false),

-- Dataset Management
    (gen_random_uuid(), 'DATASET', 'CREATE', 'Create dataset', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATASET', 'READ', 'View dataset', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'DATASET', 'UPDATE', 'Update dataset', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATASET', 'DELETE', 'Delete dataset', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'DATASET', 'EXPORT', 'Export dataset', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATASET', 'SHARE', 'Share dataset', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATASET', 'UPLOAD', 'Upload dataset', true, true, 'MEDIUM', false, false),

-- Shared Dataset (for collaboration)
    (gen_random_uuid(), 'SHARED_DATASET', 'READ', 'View shared dataset', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'SHARED_DATASET', 'COLLABORATE', 'Collaborate on shared dataset', true, true, 'MEDIUM', false, false),

-- Data Catalog Management
    (gen_random_uuid(), 'DATA_CATALOG', 'CREATE', 'Create data catalog entry', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_CATALOG', 'READ', 'View data catalog', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'DATA_CATALOG', 'UPDATE', 'Update data catalog', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_CATALOG', 'DELETE', 'Delete data catalog entry', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'DATA_CATALOG', 'MANAGE', 'Full data catalog management', true, true, 'HIGH', false, false),

-- Data Quality Management
    (gen_random_uuid(), 'DATA_QUALITY', 'CREATE', 'Create data quality rules', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_QUALITY', 'READ', 'View data quality metrics', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'DATA_QUALITY', 'UPDATE', 'Update data quality rules', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_QUALITY', 'EXECUTE', 'Execute data quality checks', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_QUALITY', 'MANAGE', 'Full data quality management', true, true, 'HIGH', false, false),

-- Data Lineage Management
    (gen_random_uuid(), 'DATA_LINEAGE', 'READ', 'View data lineage', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'DATA_LINEAGE', 'CREATE', 'Create lineage mappings', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_LINEAGE', 'UPDATE', 'Update lineage mappings', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_LINEAGE', 'MANAGE', 'Full data lineage management', true, true, 'HIGH', false, false),

-- Metadata Management
    (gen_random_uuid(), 'METADATA', 'CREATE', 'Create metadata', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'METADATA', 'READ', 'View metadata', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'METADATA', 'UPDATE', 'Update metadata', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'METADATA', 'DELETE', 'Delete metadata', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'METADATA', 'MANAGE', 'Full metadata management', true, true, 'HIGH', false, false),

-- Data Ingestion
    (gen_random_uuid(), 'DATA_INGESTION', 'CREATE', 'Create ingestion pipeline', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_INGESTION', 'READ', 'View ingestion status', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'DATA_INGESTION', 'UPDATE', 'Update ingestion pipeline', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_INGESTION', 'EXECUTE', 'Execute data ingestion', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_INGESTION', 'UPLOAD', 'Upload data files', true, true, 'MEDIUM', false, false),

-- Data Transformation
    (gen_random_uuid(), 'DATA_TRANSFORMATION', 'CREATE', 'Create transformation rules', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_TRANSFORMATION', 'READ', 'View transformation rules', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'DATA_TRANSFORMATION', 'UPDATE', 'Update transformation rules', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_TRANSFORMATION', 'EXECUTE', 'Execute data transformation', true, true, 'MEDIUM', false, false),

-- Statistical Engine
    (gen_random_uuid(), 'STATISTICAL_ENGINE', 'READ', 'View statistical engine status', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'STATISTICAL_ENGINE', 'EXECUTE', 'Execute statistical computations', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'STATISTICAL_ENGINE', 'CONFIGURE', 'Configure statistical engine', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'STATISTICAL_ENGINE', 'MANAGE', 'Full statistical engine management', true, true, 'HIGH', true, false),

-- Machine Learning Pipeline
    (gen_random_uuid(), 'ML_PIPELINE', 'CREATE', 'Create ML pipeline', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ML_PIPELINE', 'READ', 'View ML pipeline', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'ML_PIPELINE', 'UPDATE', 'Update ML pipeline', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ML_PIPELINE', 'EXECUTE', 'Execute ML pipeline', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ML_PIPELINE', 'DELETE', 'Delete ML pipeline', true, true, 'HIGH', false, false),

-- Analysis Template
    (gen_random_uuid(), 'ANALYSIS_TEMPLATE', 'CREATE', 'Create analysis template', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ANALYSIS_TEMPLATE', 'READ', 'View analysis template', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'ANALYSIS_TEMPLATE', 'UPDATE', 'Update analysis template', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ANALYSIS_TEMPLATE', 'EXECUTE', 'Execute analysis template', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ANALYSIS_TEMPLATE', 'DELETE', 'Delete analysis template', true, true, 'HIGH', false, false),

-- Report Management
    (gen_random_uuid(), 'REPORT', 'CREATE', 'Create report', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'REPORT', 'READ', 'View report', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'REPORT', 'UPDATE', 'Update report', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'REPORT', 'DELETE', 'Delete report', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'REPORT', 'EXPORT', 'Export report', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'REPORT', 'SHARE', 'Share report', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'REPORT', 'CREATE_REPORT', 'Create custom reports', true, true, 'MEDIUM', false, false),

-- Published Analysis
    (gen_random_uuid(), 'PUBLISHED_ANALYSIS', 'READ', 'View published analysis', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'PUBLISHED_ANALYSIS', 'EXPORT', 'Export published analysis', true, true, 'LOW', false, false),

-- Custom Methodology
    (gen_random_uuid(), 'CUSTOM_METHODOLOGY', 'CREATE', 'Create custom methodology', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'CUSTOM_METHODOLOGY', 'READ', 'View custom methodology', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'CUSTOM_METHODOLOGY', 'UPDATE', 'Update custom methodology', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'CUSTOM_METHODOLOGY', 'DELETE', 'Delete custom methodology', true, true, 'HIGH', false, true),

-- Model Deployment
    (gen_random_uuid(), 'MODEL_DEPLOYMENT', 'CREATE', 'Deploy models', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'MODEL_DEPLOYMENT', 'READ', 'View deployed models', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'MODEL_DEPLOYMENT', 'UPDATE', 'Update model deployment', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'MODEL_DEPLOYMENT', 'DEPLOY', 'Deploy model to production', true, true, 'HIGH', true, false),

-- Basic Statistics
    (gen_random_uuid(), 'BASIC_STATISTICS', 'READ', 'View basic statistics', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'BASIC_STATISTICS', 'EXECUTE', 'Execute basic statistical operations', true, true, 'LOW', false, false),

-- Privacy Settings
    (gen_random_uuid(), 'PRIVACY_SETTINGS', 'CREATE', 'Create privacy settings', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'PRIVACY_SETTINGS', 'READ', 'View privacy settings', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'PRIVACY_SETTINGS', 'UPDATE', 'Update privacy settings', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'PRIVACY_SETTINGS', 'MANAGE', 'Full privacy settings management', true, true, 'CRITICAL', true, false),

-- Compliance Management
    (gen_random_uuid(), 'COMPLIANCE', 'READ', 'View compliance status', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'COMPLIANCE', 'AUDIT', 'Perform compliance audit', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'COMPLIANCE', 'REPORT', 'Generate compliance reports', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'COMPLIANCE', 'MANAGE', 'Full compliance management', true, true, 'CRITICAL', true, false),

-- PII Management
    (gen_random_uuid(), 'PII_MANAGEMENT', 'READ', 'View PII policies', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'PII_MANAGEMENT', 'CREATE', 'Create PII policies', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'PII_MANAGEMENT', 'UPDATE', 'Update PII policies', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'PII_MANAGEMENT', 'MANAGE', 'Full PII management', true, true, 'CRITICAL', true, false),

-- Encryption Management
    (gen_random_uuid(), 'ENCRYPTION', 'READ', 'View encryption status', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ENCRYPTION', 'CONFIGURE', 'Configure encryption settings', true, true, 'CRITICAL', true, false),
    (gen_random_uuid(), 'ENCRYPTION', 'MANAGE', 'Full encryption management', true, true, 'CRITICAL', true, false),

-- Differential Privacy
    (gen_random_uuid(), 'DIFFERENTIAL_PRIVACY', 'READ', 'View differential privacy settings', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DIFFERENTIAL_PRIVACY', 'CONFIGURE', 'Configure differential privacy', true, true, 'HIGH', true, false),
    (gen_random_uuid(), 'DIFFERENTIAL_PRIVACY', 'APPLY', 'Apply differential privacy', true, true, 'HIGH', false, false),

-- Disclosure Risk Assessment
    (gen_random_uuid(), 'DISCLOSURE_RISK', 'READ', 'View disclosure risk assessments', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DISCLOSURE_RISK', 'ASSESS', 'Perform disclosure risk assessment', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'DISCLOSURE_RISK', 'MANAGE', 'Manage disclosure risk policies', true, true, 'HIGH', true, false),

-- Collaboration Management
    (gen_random_uuid(), 'COLLABORATION', 'READ', 'View collaboration settings', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'COLLABORATION', 'CREATE', 'Create collaboration spaces', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'COLLABORATION', 'MANAGE', 'Manage collaboration settings', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'COLLABORATION', 'COLLABORATE', 'Participate in collaboration', true, true, 'LOW', false, false),

-- Data Sharing Agreement
    (gen_random_uuid(), 'DATA_SHARING_AGREEMENT', 'CREATE', 'Create data sharing agreements', true, true, 'HIGH', false, true),
    (gen_random_uuid(), 'DATA_SHARING_AGREEMENT', 'READ', 'View data sharing agreements', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DATA_SHARING_AGREEMENT', 'UPDATE', 'Update data sharing agreements', true, true, 'HIGH', false, true),
    (gen_random_uuid(), 'DATA_SHARING_AGREEMENT', 'APPROVE', 'Approve data sharing agreements', true, true, 'HIGH', true, false),

-- Workflow Approval
    (gen_random_uuid(), 'WORKFLOW_APPROVAL', 'READ', 'View workflow approvals', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'WORKFLOW_APPROVAL', 'APPROVE', 'Approve workflows', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'WORKFLOW_APPROVAL', 'REJECT', 'Reject workflows', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'WORKFLOW_APPROVAL', 'MANAGE', 'Manage approval workflows', true, true, 'HIGH', false, false),

-- Collaborative Analysis
    (gen_random_uuid(), 'COLLABORATIVE_ANALYSIS', 'READ', 'View collaborative analysis', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'COLLABORATIVE_ANALYSIS', 'COLLABORATE', 'Participate in collaborative analysis', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'COLLABORATIVE_ANALYSIS', 'COMMENT', 'Comment on collaborative analysis', true, true, 'LOW', false, false),

-- Dashboard Management
    (gen_random_uuid(), 'DASHBOARD', 'CREATE', 'Create dashboard', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DASHBOARD', 'READ', 'View dashboard', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'DASHBOARD', 'UPDATE', 'Update dashboard', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DASHBOARD', 'DELETE', 'Delete dashboard', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DASHBOARD', 'PUBLISH', 'Publish dashboard', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'DASHBOARD', 'SHARE', 'Share dashboard', true, true, 'MEDIUM', false, false),

-- Visualization Management
    (gen_random_uuid(), 'VISUALIZATION', 'CREATE', 'Create visualization', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'VISUALIZATION', 'READ', 'View visualization', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'VISUALIZATION', 'UPDATE', 'Update visualization', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'VISUALIZATION', 'DELETE', 'Delete visualization', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'VISUALIZATION', 'EXPORT', 'Export visualization', true, true, 'LOW', false, false),

-- Chart Library
    (gen_random_uuid(), 'CHART_LIBRARY', 'READ', 'View chart library', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'CHART_LIBRARY', 'CREATE', 'Create chart templates', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'CHART_LIBRARY', 'UPDATE', 'Update chart templates', true, true, 'MEDIUM', false, false),

-- Export Management
    (gen_random_uuid(), 'EXPORT', 'EXECUTE', 'Execute data exports', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'EXPORT', 'SCHEDULE', 'Schedule exports', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'EXPORT', 'MANAGE', 'Manage export settings', true, true, 'HIGH', false, false),

-- Analysis Review
    (gen_random_uuid(), 'ANALYSIS_REVIEW', 'READ', 'View analysis for review', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'ANALYSIS_REVIEW', 'REVIEW', 'Review analysis', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'ANALYSIS_REVIEW', 'COMMENT', 'Add review comments', true, true, 'LOW', false, false),

-- Publication Approval
    (gen_random_uuid(), 'PUBLICATION_APPROVAL', 'READ', 'View publication requests', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'PUBLICATION_APPROVAL', 'APPROVE', 'Approve publications', true, true, 'HIGH', false, false),
    (gen_random_uuid(), 'PUBLICATION_APPROVAL', 'REJECT', 'Reject publications', true, true, 'MEDIUM', false, false),
    (gen_random_uuid(), 'PUBLICATION_APPROVAL', 'MANAGE', 'Manage publication workflow', true, true, 'HIGH', false, false),

-- Public Report
    (gen_random_uuid(), 'PUBLIC_REPORT', 'READ', 'View public reports', true, true, 'LOW', false, false),
    (gen_random_uuid(), 'PUBLIC_REPORT', 'VIEW', 'View public report content', true, true, 'LOW', false, false);

-- Create system super admin role (tenant-independent)
INSERT INTO roles (id, tenant_id, name, description, is_system, is_active, priority, created_by)
VALUES
    (gen_random_uuid(), NULL, 'SUPER_ADMIN', 'System-wide administrator with full access', true, true, 10000, 'SYSTEM');