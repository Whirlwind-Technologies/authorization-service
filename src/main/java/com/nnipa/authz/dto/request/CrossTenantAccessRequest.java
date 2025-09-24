package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossTenantAccessRequest {

    @NotNull(message = "Source tenant ID is required")
    private UUID sourceTenantId;

    @NotNull(message = "Target tenant ID is required")
    private UUID targetTenantId;

    @NotNull(message = "Resource type is required")
    private String resourceType;

    private String resourceId;

    @NotNull(message = "Permissions list is required")
    private List<String> permissions;

    private Map<String, Object> conditions;
    private Instant expiresAt;

    @NotNull(message = "Granted by is required")
    private String grantedBy;
}