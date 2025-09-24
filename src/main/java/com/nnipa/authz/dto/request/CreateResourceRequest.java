package com.nnipa.authz.dto.request;

import com.nnipa.authz.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResourceRequest {

    @NotNull(message = "Tenant ID is required")
    private UUID tenantId;

    @NotBlank(message = "Resource identifier is required")
    private String resourceIdentifier;

    @NotNull(message = "Resource type is required")
    private ResourceType resourceType;

    @NotBlank(message = "Resource name is required")
    private String name;

    private String description;
    private UUID ownerId;
    private UUID parentResourceId;
    private Map<String, Object> metadata;
    private Boolean isPublic;
    private String createdBy;
}