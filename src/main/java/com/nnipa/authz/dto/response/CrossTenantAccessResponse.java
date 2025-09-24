package com.nnipa.authz.dto.response;

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
public class CrossTenantAccessResponse {
    private UUID id;
    private UUID sourceTenantId;
    private UUID targetTenantId;
    private String resourceType;
    private String resourceId;
    private List<String> permissions;
    private Map<String, Object> conditions;
    private String grantedBy;
    private Instant grantedAt;
    private String revokedBy;
    private Instant revokedAt;
    private Instant expiresAt;
    private Boolean isActive;
}