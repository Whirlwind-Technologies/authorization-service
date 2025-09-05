package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionsRequest {

    @NotEmpty(message = "Permission IDs are required")
    private Set<UUID> permissionIds;

    @NotNull(message = "Assigned by is required")
    private String assignedBy;
}