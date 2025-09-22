package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for assigning a role to a user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Role ID is required")
    private UUID roleId;

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    private String assignedBy;

    private String roleName;

    private Instant expiresAt;
}