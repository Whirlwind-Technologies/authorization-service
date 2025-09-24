package com.nnipa.authz.dto.response;

import com.nnipa.authz.enums.PolicyEffect;
import com.nnipa.authz.enums.PolicyType;
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
public class PolicyResponse {
    private UUID id;
    private UUID tenantId;
    private String name;
    private String description;
    private PolicyType policyType;
    private PolicyEffect effect;
    private Integer priority;
    private Map<String, Object> conditions;
    private List<PermissionResponse> permissions;
    private List<ResourceResponse> resources;
    private Boolean isActive;
    private Instant startDate;
    private Instant endDate;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}