package com.nnipa.authz.enums;

/**
 * Types of resources that can be protected.
 */
public enum ResourceType {
    // System Resources
    SYSTEM,
    SERVICE,
    API_ENDPOINT,

    // Data Resources
    DATABASE,
    TABLE,
    DATASET,
    FILE,

    // Analytics Resources
    ANALYSIS,
    REPORT,
    DASHBOARD,
    VISUALIZATION,

    // Administrative Resources
    TENANT,
    USER,
    ROLE,
    PERMISSION,
    POLICY,

    // Application Resources
    FEATURE,
    MODULE,
    WORKFLOW,
    TEMPLATE,

    // Custom
    CUSTOM
}