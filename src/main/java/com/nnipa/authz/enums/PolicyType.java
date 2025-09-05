package com.nnipa.authz.enums;

/**
 * Types of authorization policies.
 */
public enum PolicyType {
    RESOURCE_BASED,  // Attached to specific resources
    IDENTITY_BASED,  // Attached to users/roles
    ATTRIBUTE_BASED, // Based on attributes (ABAC)
    TIME_BASED,      // Time-restricted access
    CONDITIONAL      // Complex conditional logic
}