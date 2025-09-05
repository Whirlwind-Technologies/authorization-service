package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for creating a role.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Role name is required")
    @Size(max = 100, message = "Role name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    private UUID parentRoleId;

    private Set<UUID> permissionIds;

    @Builder.Default
    private Integer priority = 100;

    private Integer maxUsers;

    @NotNull(message = "Created by is required")
    private String createdBy;
}