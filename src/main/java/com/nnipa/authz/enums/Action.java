package com.nnipa.authz.enums;

/**
 * Standard actions that can be performed on resources.
 */
public enum Action {
    // CRUD Operations
    CREATE,
    READ,
    UPDATE,
    DELETE,

    // Administrative Actions
    MANAGE,
    GRANT,
    REVOKE,
    APPROVE,
    REJECT,

    // Data Actions
    VIEW,
    EDIT,
    EXPORT,
    IMPORT,
    SHARE,

    // Analytics Actions
    ANALYZE,
    EXECUTE,
    SCHEDULE,

    // System Actions
    ACCESS,
    CONFIGURE,
    MONITOR,
    AUDIT
}