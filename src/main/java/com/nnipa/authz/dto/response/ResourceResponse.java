package com.nnipa.authz.dto.response;

import com.nnipa.authz.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {
    private UUID id;
    private UUID tenantId;
    private String resourceIdentifier;
    private ResourceType resourceType;
    private String name;
    private String description;
    private UUID ownerId;
    private UUID parentResourceId;
    private String parentResourceName;
    private Map<String, Object> attributes;
    private Boolean isPublic;
    private Integer policyCount;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}