package com.nnipa.authz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleResponse {
    private UUID id;
    private UUID userId;
    private String userName;
    private UUID roleId;
    private String roleName;
    private UUID tenantId;
    private String tenantName;
    private String assignedBy;
    private Instant assignedAt;
    private Instant expiresAt;
    private Boolean isActive;
}