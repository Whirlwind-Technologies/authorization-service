package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for authorization checks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    // Add this field for cross-tenant access checks
    private UUID targetTenantId;

    @NotNull(message = "Resource is required")
    private String resource;

    @NotNull(message = "Action is required")
    private String action;

    private String resourceId;

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    private String ipAddress;

    private String userAgent;
}