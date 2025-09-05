package com.nnipa.authz.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for role information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private UUID id;

    private UUID tenantId;

    private String name;

    private String description;

    private UUID parentRoleId;

    private String parentRoleName;

    private List<PermissionResponse> permissions;

    private Integer priority;

    private Integer maxUsers;

    private Long currentUsers;

    private boolean isSystem;

    private boolean isActive;

    private Instant createdAt;

    private Instant updatedAt;
}