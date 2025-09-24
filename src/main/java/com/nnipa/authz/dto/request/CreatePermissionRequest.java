package com.nnipa.authz.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    @NotBlank(message = "Action is required")
    private String action;

    private String description;
    private Boolean isSystem;
    private String riskLevel;
    private Boolean requiresMfa;
    private Boolean requiresApproval;
}